package ru.grouptable.entity;

import javax.persistence.*;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "persons")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "birth_date")
    private String birthDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "person_parents",
        joinColumns = @JoinColumn(name = "child_id"),
        inverseJoinColumns = @JoinColumn(name = "parent_id")
    )
    private Set<Person> parents = new HashSet<>();

    @ManyToMany(mappedBy = "parents", fetch = FetchType.EAGER)
    private Set<Person> children = new HashSet<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Set<Person> getParents() {
        return parents;
    }

    public void setParents(Set<Person> parents) {
        this.parents = parents;
    }

    public Set<Person> getChildren() {
        return children;
    }

    public void setChildren(Set<Person> children) {
        this.children = children;
    }

    public Set<Person> getSiblings() {
        Set<Person> siblings = new HashSet<>();
        for (Person parent : parents) {
            siblings.addAll(parent.getChildren());
        }
        siblings.remove(this); // Remove self from siblings
        return siblings;
    }

    public Set<Person> getUnclesAndAunts() {
        Set<Person> unclesAndAunts = new HashSet<>();
        for (Person parent : parents) {
            unclesAndAunts.addAll(parent.getSiblings());
        }
        return unclesAndAunts;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
} 