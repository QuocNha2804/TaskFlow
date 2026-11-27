package com.taskflow.service;

import com.taskflow.dao.JdbcDao;
import com.taskflow.config.AppSettings;
import com.taskflow.model.User;
import com.taskflow.util.*;
import com.taskflow.exception.*;
import java.time.LocalDateTime;
import java.security.*;
import java.util.*;

public final class AuthService {
    private final JdbcDao db; private final AppSettings settings;
    private volatile User current;
    public AuthService(JdbcDao db,AppSettings settings) { this.db=db; this.settings=settings; }
    public User user() { if(current==null) throw new AppException("Vui lòng đăng nhập."); return current; }
    public User login(String username,String password,boolean remember) {
        var rows=db.query("SELECT * FROM users WHERE username=?",username.strip());
        if(rows.isEmpty()||!PasswordUtil.verify(password,JdbcDao.string(rows.getFirst(),"password_hash"))) throw new ValidationException("Tên đăng nhập hoặc mật khẩu không đúng.");
        logout(); current=User.from(rows.getFirst());
        if(remember) {
            byte[] random=new byte[32]; new SecureRandom().nextBytes(random);
            String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            db.update("INSERT INTO user_sessions(token_hash,user_id,expires_at) VALUES(?,?,?)",digest(token),current.id(),LocalDateTime.now().plusDays(30));
            settings.token(token);
        }
        return current;
    }
    public boolean restore() {
        if(settings.token().isBlank()) return false;
        var rows=db.query("SELECT u.* FROM users u JOIN user_sessions s ON s.user_id=u.id WHERE s.token_hash=? AND s.expires_at>?",digest(settings.token()),LocalDateTime.now());
        if(rows.isEmpty()) { settings.token(null); return false; }
        current=User.from(rows.getFirst()); return true;
    }
    public void logout() {
        if(!settings.token().isBlank()) db.update("DELETE FROM user_sessions WHERE token_hash=?",digest(settings.token()));
        settings.token(null); current=null;
    }
    public void register(String username,String email,String fullName,String password) {
        username=Validator.required(username,"Tên đăng nhập",50);
        if(!username.matches("[A-Za-z0-9_.-]{3,50}")) throw new ValidationException("Tên đăng nhập: 3–50 chữ cái, số, dấu _ . -");
        Validator.email(email); fullName=Validator.required(fullName,"Họ tên",100);
        if(!db.query("SELECT id FROM users WHERE username=? OR email=?",username,email.strip()).isEmpty()) throw new ValidationException("Tên đăng nhập hoặc email đã tồn tại.");
        db.insert("INSERT INTO users(username,email,full_name,password_hash) VALUES(?,?,?,?)",username,email.strip(),fullName,PasswordUtil.hash(password));
    }
    public void profile(String name,String color,String avatarUrl) {
        db.update("UPDATE users SET full_name=?,avatar_color=?,avatar_url=? WHERE id=?",Validator.required(name,"Họ tên",100),color,avatarUrl,user().id());
        current=User.from(db.query("SELECT * FROM users WHERE id=?",current.id()).getFirst());
    }
    public void password(String oldPassword,String newPassword) {
        var row=db.query("SELECT password_hash FROM users WHERE id=?",user().id()).getFirst();
        if(!PasswordUtil.verify(oldPassword,JdbcDao.string(row,"password_hash"))) throw new ValidationException("Mật khẩu hiện tại không đúng.");
        db.transaction(()->{ db.update("UPDATE users SET password_hash=? WHERE id=?",PasswordUtil.hash(newPassword),user().id()); db.update("DELETE FROM user_sessions WHERE user_id=?",user().id()); return null; });
        settings.token(null);
    }
    private static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
