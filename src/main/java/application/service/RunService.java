package application.service;


import application.dto.RunDto;
import application.dto.Statistic;
import application.model.Run;
import application.model.User;
import application.repository.RunRepositoy;
import application.repository.UserRepository;
import application.utility.AppUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;


@Service
@Transactional
public class RunService {
    @Autowired
    private RunRepositoy runRepositoy;

    @Autowired
    private UserRepository userRepository;

    public List<RunDto> findRunAll() {
        List<Run> runs = runRepositoy.fetchAll();
        List<RunDto> runDtos = new ArrayList<>();

        for (Run run: runs) {
            RunDto runDto = new RunDto();
            User user = userRepository.findById(run.getAthleteId()).orElse(null);
            runDto.setAthleteId(run.getAthleteId());
            runDto.setDate(run.getDate());
            runDto.setDistance(run.getDistance());
            double a = run.getPace();
            double a1 = Math.round(a*100.0)/100.0;
            runDto.setPace(a1);
            runDto.setMovingTime(run.getMovingTime());
            runDto.setUser(user);
            runDtos.add(runDto);
        }
        return runDtos;
    }

    public List<Statistic> statistic(String fromDate, String toDate) {
        LocalDate from = null;
        LocalDate to = null;

         from = LocalDate.parse(fromDate);
         to = LocalDate.parse(toDate);

        List<Run> runs = runRepositoy.statistic(from, to);
        List<Statistic> statistics = new ArrayList<>();


        HashMap<Long, Statistic> map = new HashMap<>();
        HashMap<Long, Integer> count = new HashMap<>();
        HashMap<Long, LocalDate> localDate = new HashMap<>();



        for (int i = 0; i < runs.size(); i++) {
            Long athleteId = runs.get(i).getAthleteId();
            LocalDate dateInRecord = runs.get(i).getDate();
            if (localDate.get(athleteId) != null && dateInRecord.isEqual(localDate.get(athleteId))) {
                continue;
            }
            if (map.containsKey(athleteId) && count.containsKey(athleteId)) {
                Statistic statistic = map.get(athleteId);
                statistic.setAthleteId(athleteId);
                statistic.setDistance(runs.get(i).getDistance() + map.get(athleteId).getDistance());
                statistic.setTotalPoint(runs.get(i).getTotalPoint() + map.get(athleteId).getTotalPoint());
                LocalDate runLocalDate = runs.get(i).getDate();
                LocalDate mapLocalDate = map.get(athleteId).getDate();
                if ((runLocalDate.getDayOfMonth() != mapLocalDate.getDayOfMonth()) || (runLocalDate.getMonthValue() != mapLocalDate.getMonthValue())) {
                    statistic.setRuns(map.get(athleteId).getRuns() + 1);
                }
                statistic.setAvgPace((runs.get(i).getPace() + map.get(athleteId).getAvgPace()));
                map.put(athleteId, statistic);

                count.compute(athleteId, (k, v) -> v + 1);
                localDate.replace(athleteId, dateInRecord);
            } else {
                Statistic statistic = new Statistic();
                statistic.setAthleteId(athleteId);
                statistic.setDistance(runs.get(i).getDistance());
                statistic.setTotalPoint(runs.get(i).getTotalPoint());
                statistic.setDate(runs.get(i).getDate());
                statistic.setRuns(1);
                statistic.setAvgPace(runs.get(i).getPace());
                map.put(athleteId, statistic);
                count.put(athleteId, 1);
                localDate.put(athleteId, dateInRecord);
            }
        }

        for (Map.Entry<Long, Statistic> entry : map.entrySet())
        {
            Statistic statistic = entry.getValue();
            int total = count.get(statistic.getAthleteId());
            User user = userRepository.findByAthleteId(statistic.getAthleteId()).orElse(null);
            statistic.setUser(user);
            statistic.setId(statistic.getId());
            statistic.setAvgPace(statistic.getAvgPace()/total);
            statistics.add(statistic);

        }

        return statistics;
    }

}
