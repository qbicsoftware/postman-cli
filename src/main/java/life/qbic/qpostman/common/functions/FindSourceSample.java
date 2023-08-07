package life.qbic.qpostman.common.functions;

import static java.util.Objects.requireNonNull;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class FindSourceSample implements Function<Sample, Sample> {
    private final String sourceSampleTypeCode;

    public FindSourceSample(String sourceSampleTypeCode) {
        this.sourceSampleTypeCode = sourceSampleTypeCode;
    }

    @Override
    public Sample apply(Sample sample) {
        requireNonNull(sample, "sample must not be null");
        return findSourceSample(sample).orElse(sample);
    }

    private Optional<Sample> findSourceSample(Sample sample) {
        if (sample.getType().getCode().equals(sourceSampleTypeCode)) {
            return Optional.of(sample);
        }
        List<Sample> parents = sample.getParents();
        if (parents.isEmpty()) {
            return Optional.empty();
        }
        for (Sample parent : parents) {
            Optional<Sample> sourceSample = findSourceSample(parent);
            if (sourceSample.isPresent()) {
                return sourceSample;
            }
        }
        return Optional.empty();
    }
}
