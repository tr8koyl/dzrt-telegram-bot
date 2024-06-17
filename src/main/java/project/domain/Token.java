package project.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "token_tb")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    private String tokenCode;

    private Long relatedUserId;

    private LocalDate generationDate;

    private LocalDate expirationDate;

}