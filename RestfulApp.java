package ys09.api;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.engine.application.CorsFilter;
import org.restlet.routing.Router;
import org.restlet.service.CorsService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * The Restlet App, mapping URL patterns to ServerSideResources.
 */
public class RestfulApp extends Application {

	@Override
	public synchronized Restlet createInboundRoot() {

		// Create a Restlet router that defines routes
		final Router router = new Router(getContext());

		// Add a CORS filter to allow cross-domain requests
		CorsFilter corsFilter = new CorsFilter(getContext(), router);
		Set s2 = new HashSet<>();
		s2.add("*");
		corsFilter.setAllowedOrigins(s2);
		Set s = new HashSet<>();
		s.add("auth");
		corsFilter.setAllowedHeaders(s);
		Set s1 = new HashSet<>();
		//corsFilter.setExposedHeaders(new HashSet<String>(Arrays.asList("*")));
		corsFilter.setAllowedCredentials(true);
		// Magic
		corsFilter.setSkippingResourceForCorsOptions(true);

		// Setup up resource routing
		// ...

		  // Important!

		//GET
		router.attach("/config", ConfigResource.class);

		//GET
		// Map the URI to a java endpoint
		//router.attach("/projects", ProjectsResource.class);

		// Also put limits
		router.attach("/users/{userId}/projects", ProjectsResource.class);

		router.attach("/users/{userId}/projects/{projectId}", SingleProjectResource.class);

		// Post Project for a User
		//router.attach("/users/{userId}", InsertProjectResource.class);
		// Sprints

		// POST
		// Get Unauthorized, access only to admin
		router.attach("/users", UsersResource.class);
		// POST
		router.attach("/signin", SignInResource.class);
		// POST
		router.attach("/exists", UserExistsResource.class);

		return corsFilter;
	}

}