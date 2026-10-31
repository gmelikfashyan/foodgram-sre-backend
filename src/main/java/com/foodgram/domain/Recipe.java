package com.foodgram.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private String text;

    @Column(name = "cooking_time", nullable = false)
    private Integer cookingTime;

    protected Recipe() {
    }

    public Recipe(User author, String name, String image, String text, Integer cookingTime) {
        this.author = author;
        this.name = name;
        this.image = image;
        this.text = text;
        this.cookingTime = cookingTime;
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getCookingTime() { return cookingTime; }
    public void setCookingTime(Integer cookingTime) { this.cookingTime = cookingTime; }
}
