package application;

import application.model.Run;
import application.model.Token;
import application.model.User;
import application.repository.RunRepositoy;
import application.repository.TokenRepository;
import application.repository.UserRepository;
import application.utility.ApiRequester;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.core.type.TypeReference;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Configuration
@Component
public class AuthenticationListener implements ApplicationListener<ContextRefreshedEvent> {
    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;



    private final ApiRequester apiRequester;
    private final ObjectMapper objectMapper;
    private final RunRepositoy runRepositoy;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;


    @Autowired
    public AuthenticationListener(ApiRequester apiRequester,
                                  ObjectMapper objectMapper,
                                  RunRepositoy runRepositoy,
                                  TokenRepository tokenRepository,
                                  UserRepository userRepository
    ) {
        this.apiRequester = apiRequester;
        this.objectMapper = objectMapper;
        this.runRepositoy = runRepositoy;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }


    @SneakyThrows
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) throws RuntimeException {

        System.out.println("START UPDATE TOKEN:" + System.currentTimeMillis());
        List<Token> tokens = tokenRepository.findAll();
        userRepository.deleteAll();

        for (Token token : tokens) {
            JsonNode jsonNode;
            String uri = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("/oauth/token")
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("refresh_token", token.getRefresh())
                    .queryParam("grant_type", "refresh_token")
                    .toUriString();
            ResponseEntity<String> response = apiRequester.sendGetRequestForRefreshToken(uri);
            if (response == null) continue;
            String body = response.getBody();

            jsonNode = objectMapper.readValue(body, new TypeReference<JsonNode>() {});

            Token updateToken = tokenRepository.findById(token.getId()).orElse(null);
            updateToken.setAccess(jsonNode.get("access_token").asText());
            updateToken.setRefresh(jsonNode.get("refresh_token").asText());
            tokenRepository.save(updateToken);


            JsonNode userNode;
            String userUri = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("api/v3/athlete")
                    .toUriString();
            ResponseEntity<String> userResponse = apiRequester.sendGetRequest(token.getAccess(), userUri);
            if (userResponse == null) continue;

            String userBody = userResponse.getBody();
            try {
                userNode = objectMapper.readValue(userBody, new TypeReference<JsonNode>() {
                });
            } catch (Exception ex) {
                continue;
            }
            User user = new User();
            String name = (userNode.get("lastname") + " " + userNode.get("firstname")).replace('\"',' ');
            user.setAthleteId(userNode.get("id").asLong());
            user.setName(name);
            user.setImage(userNode.get("profile_medium").asText());
            userRepository.save(user);


            List<JsonNode> jsons;
            String uri1 = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("/api/v3/athlete/activities")
                    .toUriString();
            ResponseEntity<String> response1 = apiRequester.sendGetRequest(updateToken.getAccess(), uri1);
            String body1 = response1.getBody();
            try {
                jsons = objectMapper.readValue(body1, new TypeReference<List<JsonNode>>() {
                });
            }catch (Exception ex){
                continue;
            }
            for (JsonNode node : jsons) {
                Run run = new Run();
                double distance = node.get("distance").asDouble();
                long movingTime = node.get("moving_time").asLong();
                double avgPace =  (movingTime/60)/(distance/1000);
                String date = node.get("start_date_local").asText();
                String type = node.get("type").asText();

                double point =0 ;
                if(avgPace>=3 && avgPace<6.5){
                    point = (avgPace*0.2*3) + (distance/1000)*0.3 + 0.5;
                }
                if(avgPace>=6.5 && avgPace<9){
                    point = (avgPace*0.2*2) + (distance/1000)*0.3 + 0.5;
                }
                if(avgPace>=9 && avgPace<=15){
                    point = (avgPace*0.2*1) + (distance/1000)*0.3 + 0.5;
                }
                System.out.println("test:"+point);
                String[] splitDate = date.split("T");
                LocalDate localDate = LocalDate.parse(splitDate[0]);
                String dateStartVerTwo = "2022-04-22";
                LocalDate dateStartVerTwoFormat = LocalDate.parse(dateStartVerTwo);
//                String dateStop = "2021-05-25";
//                LocalDate dateFormat = LocalDate.parse(dateStop);
//
//                String dateContinue = "2021-07-04";
//                String dateStopContinue = "2021-07-09";
//                LocalDate dateContinueFormat = LocalDate.parse(dateContinue);
//                LocalDate dateStopContinueFormat = LocalDate.parse(dateStopContinue);

//                String dateCovidContinue = "2022-04-22";
//                LocalDate dateCovidContinueFormat = LocalDate.parse(dateCovidContinue);
                if (((localDate.isAfter(dateStartVerTwoFormat))
//                        && (localDate.isBefore(dateStopFormat))
                        && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.0 ) && (type.equals("Run")))
//                        || ((localDate.isAfter(dateContinueFormat)) && (localDate.isBefore(dateStopContinueFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))
//                        || ((localDate.isAfter(dateCovidContinueFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))
                ) {
                    run.setAthleteId(token.getAthleteId());
                    run.setDistance(distance);
                    run.setMovingTime(movingTime);
                    run.setPace(avgPace);
                    run.setDate(localDate);
                    run.setTotalPoint(point);
                    List<Run> paceDB = runRepositoy.findAllByPaceAndDate(run.getPace(), run.getDate() );
                    if(paceDB.size()==0){
                        runRepositoy.save(run);
//                Run run = new Run();
//                double distance = node.get("distance").asDouble();
//                long movingTime = node.get("moving_time").asLong();
//                double avgPace =  (movingTime/60)/(distance/1000);
//                String date = node.get("start_date_local").asText();
//                String type = node.get("type").asText();
//                String[] splitDate = date.split("T");
//                LocalDate localDate = LocalDate.parse(splitDate[0]);
//                String date1 = "2021-03-28";
//                String dateStop = "2021-05-25";
//                LocalDate dateStopFormat = LocalDate.parse(dateStop);
//                LocalDate dateFormat = LocalDate.parse(date1);
//
//                String dateContinue = "2021-07-04";
//                String dateStopContinue = "2021-07-09";
//                LocalDate dateContinueFormat = LocalDate.parse(dateContinue);
//                LocalDate dateStopContinueFormat = LocalDate.parse(dateStopContinue);
//                if (((localDate.isAfter(dateFormat)) && (localDate.isBefore(dateStopFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))
//                        || ((localDate.isAfter(dateContinueFormat)) && (localDate.isBefore(dateStopContinueFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))) {
//                    run.setAthleteId(token.getAthleteId());
//                    run.setDistance(distance);
//                    run.setMovingTime(movingTime);
//                    run.setPace(avgPace);
//                    run.setDate(localDate);
//                    List<Run> paceDB = runRepositoy.findAllByPaceAndDate(run.getPace(), run.getDate() );
//                    if(paceDB.size()==0){
//                        runRepositoy.save(run);
                    }
                }
            }
        }

    }

}

/**
@Component
public class AuthenticationListener implements ApplicationListener<InteractiveAuthenticationSuccessEvent>{

    private final OAuth2AuthorizedClientService clientService;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApiRequester apiRequester;
    private final ObjectMapper objectMapper;
    private final RunRepositoy runRepositoy;

    @Autowired
    public AuthenticationListener(OAuth2AuthorizedClientService clientService,
                                  TokenRepository tokenRepository,
                                  UserRepository userRepository,
                                  ApiRequester apiRequester,
                                  ObjectMapper objectMapper,
                                  RunRepositoy runRepositoy
                                  ) {
        this.clientService = clientService;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.apiRequester = apiRequester;
        this.objectMapper = objectMapper;
        this.runRepositoy = runRepositoy;
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) event.getAuthentication();
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2User user = token.getPrincipal();

        Map<String, Object> map = user.getAttributes();
        DefaultOAuth2User defaultUser = (DefaultOAuth2User) event.getAuthentication().getPrincipal();

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(registrationId, defaultUser.getName());
        String access_token =  client.getAccessToken().getTokenValue();
        String refreshToken = client.getRefreshToken().getTokenValue();

        Token insertToken = new Token();
        insertToken.setAccess(access_token);
        insertToken.setRefresh(refreshToken);
        tokenRepository.save(insertToken);


        String fullName = map.get("lastname") + " " + map.get("firstname");
        String image = (String) map.get("profile");

        User insertUser = new User();
        insertUser.setName(fullName);
        insertUser.setImage(image);
        userRepository.save(insertUser);

        List<JsonNode> jsons;
        String uri = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("/api/v3/athlete/activities")
                .toUriString();
        ResponseEntity<String> response = apiRequester.sendGetRequest(client, uri);

        String body = response.getBody();

        jsons = objectMapper.readValue(body, new TypeReference<List<JsonNode>>() {});

        for (JsonNode node : jsons) {
            Run run = new Run();
            run.setDistance(node.get("distance").asDouble());
            run.setMovingTime(node.get("moving_time").asInt());
            run.setDate(node.get("start_date").asText());
            runRepositoy.save(run);
        }
    }
}
*/
