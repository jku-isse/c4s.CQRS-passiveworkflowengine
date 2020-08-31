package impactassessment.passiveprocessengine.verification;

import java.util.ArrayList;
import java.util.List;

public class Report {

    List<Warning> warnings;

    protected Report() {
        warnings = new ArrayList<>();
    }

    protected void addWarning(Warning warning) {
        warnings.add(warning);
    }
}
