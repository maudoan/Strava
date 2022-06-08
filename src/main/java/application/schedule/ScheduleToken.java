package application.schedule;

import application.dto.RunDto;
import application.model.Run;
import application.model.Token;
import application.model.User;
import application.repository.RunRepositoy;
import application.repository.TokenRepository;
import application.repository.UserRepository;
import application.utility.ApiRequester;
import application.utility.AppUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * Created By hiepnd
 * Date: 29/03/2021
 * Time: 11:18 AM
 * Contact me via mail hiepnd@vnpt-technology.vn
 */

@Configuration
@EnableScheduling
public class ScheduleToken {


    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${distanceConfig}")
    private double distanceconfig;

    @Value("${minAvgPaceConfig}")
    private double minAvgPaceconfig;

    @Value("${maxAvgPaceConfig}")
    private double maxAvgPaceconfig;

    private final TokenRepository tokenRepository;
    private final ApiRequester apiRequester;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RunRepositoy runRepositoy;

    public ScheduleToken(TokenRepository tokenRepository,
                         ApiRequester apiRequester,
                         ObjectMapper objectMapper,
                         UserRepository userRepository,
                         RunRepositoy runRepositoy) {
        this.tokenRepository = tokenRepository;
        this.apiRequester = apiRequester;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.runRepositoy = runRepositoy;
    }


    @Scheduled(cron ="0 30 23 * * *", zone = "Asia/Ho_Chi_Minh") // 18h là chạy
    public void updateToken() throws JsonProcessingException {
        System.out.println("START UPDATE TOKEN," + System.currentTimeMillis());
        List<Token> tokens = tokenRepository.findAll();

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
        }

    }
    @Scheduled(cron ="0 45 11 * * *", zone = "Asia/Ho_Chi_Minh") // 18h là chạy
    public void updateToken1() throws JsonProcessingException {
        System.out.println("START UPDATE TOKEN," + System.currentTimeMillis());
        List<Token> tokens = tokenRepository.findAll();

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
        }

    }

    @Scheduled(cron = "0 0 0 * * *",zone = "Asia/Ho_Chi_Minh")//chạy sau mỗi 0h 0p mỗi
    public void activitySync() throws JsonProcessingException {

        List<Token> tokens = tokenRepository.findAll();
        for (Token token : tokens) {
            List<JsonNode> jsons;
            String uri = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("/api/v3/athlete/activities")
                    .toUriString();
            ResponseEntity<String> response = apiRequester.sendGetRequest(token.getAccess(), uri);
            String body = response.getBody();
            try {
                jsons = objectMapper.readValue(body, new TypeReference<List<JsonNode>>() {
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
                double point = 0;
                if(avgPace>=3 && avgPace<6.5){
                    point = (avgPace*0.2*3) + (distance/1000)*0.3 + 0.5;
                }
                if(avgPace>=6.5 && avgPace<9){
                    point = (avgPace*0.2*2) + (distance/1000)*0.3 + 0.5;
                }
                if(avgPace>=9 && avgPace<=15){
                    point = (avgPace*0.2*1) + (distance/1000)*0.3 + 0.5;
                }
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
                        && (distance >= distanceconfig) && (avgPace >= minAvgPaceconfig  || avgPace <= maxAvgPaceconfig ) && (type.equals("Run")))
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
                    }
                }

            }
        }
    }
    @Scheduled(cron = "0 15 12 * * *",zone = "Asia/Ho_Chi_Minh")//chạy sau mỗi 0h 0p mỗi
    public void activitySync1() throws JsonProcessingException {
        List<Token> tokens = tokenRepository.findAll();
        for (Token token : tokens) {
            List<JsonNode> jsons;
            String uri = UriComponentsBuilder.newInstance().scheme("https").host("www.strava.com").path("/api/v3/athlete/activities")
                    .toUriString();
            ResponseEntity<String> response = apiRequester.sendGetRequest(token.getAccess(), uri);
            String body = response.getBody();
            try {
                jsons = objectMapper.readValue(body, new TypeReference<List<JsonNode>>() {
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

                String[] splitDate = date.split("T");
                LocalDate localDate = LocalDate.parse(splitDate[0]);
                String dateStartVerTwo = "2022-04-22";
                LocalDate dateStartVerTwoFormat = LocalDate.parse(dateStartVerTwo);
//                String dateStop = "2021-05-25";
//                LocalDate dateStopFormat = LocalDate.parse(dateStop);
//
//                String dateContinue = "2021-07-04";
//                String dateStopContinue = "2021-07-09";
//                LocalDate dateContinueFormat = LocalDate.parse(dateContinue);
//                LocalDate dateStopContinueFormat = LocalDate.parse(dateStopContinue);
//
//                String dateCovidContinue = "2022-04-22";
//                LocalDate dateCovidContinueFormat = LocalDate.parse(dateCovidContinue);
                if (((localDate.isAfter(dateStartVerTwoFormat))
//                        && (localDate.isBefore(dateStopFormat))
                        && (distance >= distanceconfig) && (avgPace >= minAvgPaceconfig  || avgPace <= maxAvgPaceconfig ) && (type.equals("Run")))
//                        || ((localDate.isAfter(dateContinueFormat)) && (localDate.isBefore(dateStopContinueFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))
//                        || ((localDate.isAfter(dateCovidContinueFormat)) && (distance >= 2000) && (avgPace >= 3.30  || avgPace <= 15.00 ) && (type.equals("Run")))
                ) {
                    run.setAthleteId(token.getAthleteId());
                    run.setDistance(distance);
                    run.setMovingTime(movingTime);
                    run.setPace(avgPace);
                    run.setDate(localDate);
                    List<Run> paceDB = runRepositoy.findAllByPaceAndDate(run.getPace(), run.getDate() );
                    if(paceDB.size()==0){
                        runRepositoy.save(run);
                    }
                }

            }
        }
    }
}
