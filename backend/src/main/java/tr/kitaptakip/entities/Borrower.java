package tr.kitaptakip.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "borrowers")
public class Borrower extends AuditableEntity {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "borrower", fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    protected Borrower() {
    }

    public Borrower(String firstName, String lastName, String email, String phone, String notes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
    }

    public void updateDetails(String firstName, String lastName, String email, String phone, String notes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() { return id; }
    void addLoan(Loan loan) { loans.add(loan); }
    public List<Loan> getLoans() { return List.copyOf(loans); }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
}
