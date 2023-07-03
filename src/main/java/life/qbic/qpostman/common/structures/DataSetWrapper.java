package life.qbic.qpostman.common.structures;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import java.time.Instant;
import life.qbic.qpostman.common.functions.FindSourceSample;

/**
 * Wraps a DataSet as the openBis DTOs do not guarantee equals and hash code implementation.
 */
public final class DataSetWrapper {


    private static FindSourceSample findSourceSample;
    private final ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet dataSet;
    private Sample sourceSample = null;

    public DataSetWrapper(ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet dataSet) {
        requireNonNull(dataSet, "dataSet must not be null");
        this.dataSet = dataSet;
    }

    public DataSetPermId dataSetPermId() {
        return dataSet.getPermId();
    }

    public Instant registrationTime() {
        return dataSet.getRegistrationDate().toInstant();
    }

    public String sampleCode() {
        return dataSet.getSample().getCode();
    }

    /**
     * <b>DO NOT USE FOR EQUALS AND HASH CODE</b>
     * @return the sample where this dataset is attached
     */
    public Sample sample() {
        return dataSet.getSample();
    }

    public Sample sourceSample() {
        if (nonNull(sourceSample)) {
            return sourceSample;
        }
        this.sourceSample = findSourceSample.apply(sample());
        return sourceSample;
    }

    public static void setFindSourceFunction(FindSourceSample findSourceSample) {
        DataSetWrapper.findSourceSample = findSourceSample;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSetWrapper that = (DataSetWrapper) o;

        return dataSetPermId().equals(that.dataSetPermId())
                && sampleCode().equals(that.sampleCode())
                && registrationTime().equals(that.registrationTime());
    }

    @Override
    public int hashCode() {
        int result = dataSetPermId().hashCode();
        result = 31 * result + sampleCode().hashCode();
        result = 31 * result + registrationTime().hashCode();
        return result;
    }
}
