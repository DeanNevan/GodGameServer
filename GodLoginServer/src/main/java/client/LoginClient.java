package client;

import com.alibaba.fastjson.JSONObject;

import java.time.LocalTime;

public class LoginClient extends BasicClient {

    public static enum LOGIN_STATE {
        UNKNOWN, PRE_LOGIN, LOGIN, PRE_REGISTER, REGISTER
    }

    private String encryptedPassword;
    private String ivAscii;
    private String verificationCode;
    private LOGIN_STATE loginState = LOGIN_STATE.UNKNOWN;
    private LocalTime time = LocalTime.now();

    public LoginClient(String id, String gateServerID){
        super(id, gateServerID);
    }

    public void preRegister(String verificationCode){
        this.verificationCode = verificationCode;
        setLoginState(LOGIN_STATE.PRE_REGISTER);
    }

    public void register(String encryptedPassword){
        this.encryptedPassword = encryptedPassword;
        setLoginState(LOGIN_STATE.REGISTER);
    }

    public void preLogin(String verificationCode){
        this.verificationCode = verificationCode;
        setLoginState(LOGIN_STATE.PRE_LOGIN);
    }

    public void login(String encryptedPassword){
        this.encryptedPassword = encryptedPassword;
        setLoginState(LOGIN_STATE.LOGIN);
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public LOGIN_STATE getLoginState() {
        return loginState;
    }

    public void setLoginState(LOGIN_STATE loginState) {
        this.loginState = loginState;
    }

    public String getIvAscii() {
        return ivAscii;
    }

    public void setIvAscii(String ivAscii) {
        this.ivAscii = ivAscii;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public void parseToJSONObject(JSONObject jsonObject){
        super.parseToJSONObject(jsonObject);
        if (jsonObject.getString("login_state") == null) {
            jsonObject.put("login_state", getLoginState());
        } else {
            jsonObject.replace("login_state", getLoginState());
        }
        if (jsonObject.getString("encrypted_password") == null) {
            jsonObject.put("encrypted_password", getEncryptedPassword());
        } else {
            jsonObject.replace("encrypted_password", getEncryptedPassword());
        }
        if (jsonObject.getString("iv_ascii") == null) {
            jsonObject.put("iv_ascii", getIvAscii());
        } else {
            jsonObject.replace("iv_ascii", getIvAscii());
        }
        if (jsonObject.getString("verification_code") == null) {
            jsonObject.put("verification_code", getVerificationCode());
        } else {
            jsonObject.replace("verification_code", getVerificationCode());
        }

    }

    public void parseFromJSONString(String jsonstring){
        super.parseFromJSONString(jsonstring);
        JSONObject jsonObject = JSONObject.parseObject(jsonstring);
        if (jsonObject.getString("login_state") != null) {
            setLoginState(LOGIN_STATE.valueOf(jsonObject.getString("login_state")));
        }
        if (jsonObject.getString("encrypted_password") != null) {
            setEncryptedPassword(jsonObject.getString("encrypted_password"));
        }
        if (jsonObject.getString("iv_ascii") != null) {
            setIvAscii(jsonObject.getString("iv_ascii"));
        }
        if (jsonObject.getString("verification_code") != null) {
            setVerificationCode(jsonObject.getString("verification_code"));
        }
    }
    
}
