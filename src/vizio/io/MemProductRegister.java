package vizio.io;

/**
 * Each product is independent with respect to its transaction boundaries.
 * Therefore each product is stored in a separate file.
 *
 * @author jan
 */
public class MemProductRegister {

	// INDEX A: product, status
	// INDEX B: product, area, status, user
	// INDEX C: product, version, status, user

	// INDEX D: user, status
	// INDEX E: user, motive
	// INDEX F: user, goal
	// INDEX G: user (watch)

	// queries that do not limit by user or by product are illegal for now
	// other filters and sorts are done on linearly on the set extracted from index by binary search

	// Idea: "weak transaction semantics"
	// products are in a file on their own, user as well.
	// changes to users should not conflict
	// product changes are applied, when successful user changes are saved, on conflict they are merged.
	// merge might fail. In that case the user with the latest token is kept.
	// this problem is more theoretical as only one person acts as a user.
}
