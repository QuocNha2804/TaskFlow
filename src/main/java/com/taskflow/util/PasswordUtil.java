package com.taskflow.util;
import org.mindrot.jbcrypt.BCrypt;
public final class PasswordUtil {
    private PasswordUtil() {}
    public static String hash(String password) { Validator.password(password); return BCrypt.hashpw(password,BCrypt.gensalt(12)); }
    public static boolean verify(String password,String hash) { try { return password!=null&&password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72&&BCrypt.checkpw(password,hash); } catch(IllegalArgumentException e) { return false; } }
}
