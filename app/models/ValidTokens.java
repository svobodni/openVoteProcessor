package models;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;

import javax.persistence.*;
import javax.validation.constraints.*;

@Entity
public class ValidTokens {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	@NotNull
	private Ballot ballot;
	
	@NotNull
	private Timestamp createdTime;
	
	@ElementCollection
	private Collection<String> digestValues;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Ballot getBallot() {
		return ballot;
	}

	public void setBallot(Ballot ballot) {
		this.ballot = ballot;
	}

	public Timestamp getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Timestamp createdTime) {
		this.createdTime = createdTime;
	}

	public Collection<String> getDigestValues() {
		return digestValues;
	}

	public void setDigestValues(Collection<String> digestValues) {
		this.digestValues = digestValues;
	}

}
