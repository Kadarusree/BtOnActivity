package mahesh.cubex.btonactivity;

import java.io.Serializable;

/**
 * Created by srikanthk on 3/13/2019.
 */

public class Model implements Serializable {
    byte[] image;
    String path;

    public Model(byte[] image, String path) {
        this.image = image;
        this.path = path;
    }
}
