package controllers;

import java.util.List;

import javax.persistence.TypedQuery;

import models.Ballot;
import play.*;
import play.mvc.*;
import views.html.*;
import play.db.jpa.*;

public class Application extends Controller {
	
	@Transactional(readOnly=true)
    public static Result index() {
        return ok(index.render());
    }
	
	@Transactional(readOnly=true)
    public static Result ballots() {
		TypedQuery<Ballot> query = JPA.em().createQuery("SELECT b FROM Ballot b", Ballot.class);
		List<Ballot> ballotList = query.getResultList();
        return ok(ballots.render(ballotList));
    }

	@Transactional(readOnly=true)
    public static Result vote() {
        return ok(vote.render());
    }
}
