package com.bql.feedback.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** Danh mục sự cố. keywords dùng cho phân loại tự động. */
@Entity
@Table(name = "feedback_categories")
public class FeedbackCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_group")
    private IssueGroup group;

    @Enumerated(EnumType.STRING)
    private Priority defaultPriority;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feedback_category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    private boolean active;

    protected FeedbackCategory() {
    }

    public FeedbackCategory(Long id, String code, String name, IssueGroup group,
                            Priority defaultPriority, List<String> keywords, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.group = group;
        this.defaultPriority = defaultPriority;
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public IssueGroup getGroup() { return group; }
    public void setGroup(IssueGroup group) { this.group = group; }
    public Priority getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(Priority defaultPriority) { this.defaultPriority = defaultPriority; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
