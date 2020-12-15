package com.outlookcalendar;

import com.facebook.react.bridge.ReadableMap;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.DateTimeTimeZone;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.IEventCollectionPage;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class GraphHelper implements IAuthenticationProvider {
    private static GraphHelper INSTANCE = null;
    private IGraphServiceClient mClient = null;
    private String mAccessToken = null;

    private GraphHelper() {
        mClient = GraphServiceClient.builder()
                .authenticationProvider(this).buildClient();
    }

    public static synchronized GraphHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphHelper();
        }

        return INSTANCE;
    }

    // Part of the Graph IAuthenticationProvider interface
    // This method is called before sending the HTTP request
    @Override
    public void authenticateRequest(IHttpRequest request) {
        // Add the access token in the Authorization header
        request.addHeader("Authorization", "Bearer " + mAccessToken);
    }

    public User getUser(String accessToken) {
        mAccessToken = accessToken;

        // GET /me (logged in user)
        return mClient.me().buildRequest().get();
    }

    public IEventCollectionPage getEvents(String accessToken) {
        mAccessToken = accessToken;

        // Use query options to sort by created time
        final List<Option> options = new LinkedList<>();
        options.add(new QueryOption("orderby", "createdDateTime DESC"));
        // GET /me/events
        return mClient.me().events().buildRequest(options)
                .select("subject,organizer,start,end")
                .get();
    }

    public Event saveEvent(String accessToken, ReadableMap data) throws Exception {
        mAccessToken = accessToken;
       /* ReadableMapKeySetIterator keyIterator = data.keySetIterator();
        int numKeys = 0;
        while (keyIterator.hasNextKey()) {
            numKeys++;
            keyIterator.nextKey();
        }*/

        System.out.println("accessToken: " + accessToken);
        System.out.println("subject: " + data.getString("subject"));
        Event event = new Event();

        if (!data.hasKey("subject")) {
            throw new Exception("new event require `subject` field");
        }
        if (!data.hasKey("start")) {
            throw new Exception("new event require `start` field");
        }
        if (!data.hasKey("end")) {
            throw new Exception("new event require `end` field");
        }

        event.subject = data.getString("subject");

        DateTimeTimeZone start = new DateTimeTimeZone();
        start.dateTime = data.getString("start");
        start.timeZone = TimeZone.getDefault().getID();
        event.start = start;

        DateTimeTimeZone end = new DateTimeTimeZone();
        end.dateTime = data.getString("end");
        end.timeZone = TimeZone.getDefault().getID();
        event.end = end;

        if (data.hasKey("id")) {
            System.out.println("case PATCH");
            // PATCH /me/events/{id}
            String id = data.getString("id");
            return mClient.me().events()
                    .byId(id)
                    .buildRequest()
                    .patch(event);
        } else {
            System.out.println("case POST");
            // POST /me/events
            return mClient.me().events()
                    .buildRequest()
                    .post(event);
        }
    }

    public Event getEvent(String accessToken, String id) throws Exception {
        mAccessToken = accessToken;
        return mClient.me().events().byId(id).buildRequest().get();
    }

    public void deleteEvent(String accessToken, String id) throws Exception {
        mAccessToken = accessToken;
        mClient.me().events().byId(id).buildRequest().delete();
    }
}


