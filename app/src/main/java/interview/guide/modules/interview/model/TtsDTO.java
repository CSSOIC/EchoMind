package interview.guide.modules.interview.model;

import lombok.Data;

import java.io.Serializable;
@Data
public class TtsDTO implements Serializable {
    public String Text;
    public String Voice;
    public Double rate;
}
