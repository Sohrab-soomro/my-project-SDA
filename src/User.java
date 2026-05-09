import java.io.Serializable;

public class User implements Serializable {
    private String email, password, role, phone, favoriteColor;
    private boolean isActive = true;

    public User(String email, String password, String role, String phone, String favoriteColor) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.favoriteColor = favoriteColor;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public String getFavoriteColor() { return favoriteColor; }
    public boolean isActive() { return isActive; }

    public void setActive(boolean active) { this.isActive = active; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return "[" + role + "] " + email + (isActive ? "" : " (Deactivated)");
    }
}
