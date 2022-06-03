package application.enpoint;


import application.dto.RunDto;
import application.dto.Statistic;
import application.model.Run;
import application.service.RunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1")
public class RunEndpoint {

    @Autowired
    private RunService runService;

    @RequestMapping(value="/run", method= RequestMethod.GET)
    public ResponseEntity<List<RunDto>> getRunAll(){
        List<RunDto> runs = runService.findRunAll();
        return new ResponseEntity<>(runs, HttpStatus.OK);
    }

    @RequestMapping(value="/statistic", method= RequestMethod.GET)
    public ResponseEntity<List<Statistic>> statistic(@RequestParam String fromDate, @RequestParam String toDate){
        List<Statistic> statistics = runService.statistic(fromDate, toDate);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
}
