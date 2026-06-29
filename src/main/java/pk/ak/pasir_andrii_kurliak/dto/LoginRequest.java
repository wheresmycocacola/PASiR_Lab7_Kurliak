package pk.ak.pasir_andrii_kurliak.dto;

public class LoginRequest {

    private String email;
    private String password;

    public LoginRequest() {
        // Default constructor required by Jackson for JSON deserialization.
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}