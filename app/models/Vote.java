package models;

import java.util.Collection;
import java.util.Map;

import javax.persistence.*;
import javax.validation.constraints.*;

@Entity
public class Vote {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	@NotNull
	private Ballot ballot;
	
	@NotNull
	private String tokenDigestValue;
	
	@ManyToMany(mappedBy="votes")
	private Collection<BallotOption> ballotOptions;
	
	@ElementCollection
	private Map<String,String> stats;
	
	@NotNull
	private String voteDigestValue;
	

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

	public String getTokenDigestValue() {
		return tokenDigestValue;
	}

	public void setTokenDigestValue(String tokenDigestValue) {
		this.tokenDigestValue = tokenDigestValue;
	}

	public Collection<BallotOption> getBallotOptions() {
		return ballotOptions;
	}

	public void setBallotOptions(Collection<BallotOption> ballotOptions) {
		this.ballotOptions = ballotOptions;
	}

	public Map<String, String> getStats() {
		return stats;
	}

	public void setStats(Map<String, String> stats) {
		this.stats = stats;
	}

	public String getVoteDigestValue() {
		return voteDigestValue;
	}

	public void setVoteDigestValue(String digestValue) {
		this.voteDigestValue = digestValue;
	}
}
