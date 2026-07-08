package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * The guest's contact and mailing details for a booking, collected on the
 * Review &amp; Confirm page and carried forward for the confirmation screen
 * (and, later, for the persisted reservation record).
 */
public class GuestInfo {

    private String firstName = "";
    private String lastName = "";
    private String phone = "";
    private String email = "";
    private String streetAddress = "";
    private String city = "";
    private String province = "";
    private String postalCode = "";
    private String country = "";

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }

    /** e.g. "123 Main St, Toronto, ON M5B 2K3, Canada" */
    public String fullAddress() {
        return streetAddress + ", " + city + ", " + province + " " + postalCode + ", " + country;
    }

    public void reset() {
        firstName = "";
        lastName = "";
        phone = "";
        email = "";
        streetAddress = "";
        city = "";
        province = "";
        postalCode = "";
        country = "";
    }
}
