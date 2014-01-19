package models;

import java.util.*;

import javax.persistence.*;
import javax.validation.constraints.*;

//import play.db.jpa.*;

@Entity
public class Ballot {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@NotNull
	private String externalId;

	@NotNull
	private String name;
	
	private String description;
	
	@NotNull
	@Future
	private java.sql.Timestamp startTime;
	
	@NotNull
	@Future
	private java.sql.Timestamp endTime;
	
	private boolean showVoters;
	
	@Enumerated(EnumType.STRING)
	@NotNull
	private BallotType ballotType;
	
	private int maxChecks;
	
	private String digestValue;
	
	private String signatureSubjectName;
	
	//@OneToMany(mappedBy="ballot", cascade=CascadeType.REMOVE)
	@OneToMany(mappedBy="ballot", cascade=CascadeType.REMOVE)
	private Collection<BallotOption> options;
	
	@OneToMany(mappedBy="ballot", cascade=CascadeType.REMOVE)
	@OrderBy("createdTime ASC")
	private List<ValidTokens> validTokens;
	
	@OneToMany(mappedBy="ballot", cascade=CascadeType.REMOVE)
	@ElementCollection
	private Collection<Vote> castVotes;

	public boolean isEnded() {
		return new Date().after(this.getEndTime());
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public java.sql.Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(java.sql.Timestamp startTime) {
		this.startTime = startTime;
	}

	public java.sql.Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(java.sql.Timestamp endTime) {
		this.endTime = endTime;
	}

	public boolean isShowVoters() {
		return showVoters;
	}

	public void setShowVoters(boolean showVoters) {
		this.showVoters = showVoters;
	}

	public BallotType getBallotType() {
		return ballotType;
	}

	public void setBallotType(BallotType ballotType) {
		this.ballotType = ballotType;
	}

	public int getMaxChecks() {
		return maxChecks;
	}

	public void setMaxChecks(int maxChecks) {
		this.maxChecks = maxChecks;
	}

	public String getDigestValue() {
		return digestValue;
	}

	public void setDigestValue(String digestValue) {
		this.digestValue = digestValue;
	}

	public String getSignatureSubjectName() {
		return signatureSubjectName;
	}

	public void setSignatureSubjectName(String signatureSubjectName) {
		this.signatureSubjectName = signatureSubjectName;
	}

	public Collection<BallotOption> getOptions() {
		return options;
	}

	public void setOptions(Collection<BallotOption> options) {
		this.options = options;
	}

	public List<ValidTokens> getValidTokens() {
		return validTokens;
	}

	public void setValidTokens(List<ValidTokens> validTokens) {
		this.validTokens = validTokens;
	}
	
	public ValidTokens getCurrentValidTokens() {
		if(this.getValidTokens().size()>0) {
			return this.getValidTokens().get(this.getValidTokens().size()-1);
		} else {
			ValidTokens vt = new ValidTokens();
			vt.setDigestValues(new HashSet<String>());
			return vt;
		}
	
	}

	public Collection<Vote> getCastVotes() {
		return castVotes;
	}

	public void setCastVotes(Collection<Vote> castVotes) {
		this.castVotes = castVotes;
	}
}