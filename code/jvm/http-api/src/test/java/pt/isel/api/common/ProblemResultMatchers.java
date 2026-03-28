package pt.isel.api.common;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProblemResultMatchers {

    public static ResultMatcher isProblem(Problem problem) {
        return result -> {
            status().is(problem.status.value()).match(result);
            jsonPath("$.title").value(problem.title).match(result);
            jsonPath("$.status").value(problem.status.value()).match(result);
            jsonPath("$.detail").value(problem.detail).match(result);
        };
    }
}