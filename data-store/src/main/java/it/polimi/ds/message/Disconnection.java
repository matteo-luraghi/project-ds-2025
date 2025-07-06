package it.polimi.ds.message;

import java.io.Serial;
import java.io.Serializable;

/**
 * Disconnecion
 *
 * <p>notifies the disconnection of a client
 */
public class Disconnection implements Serializable {
  @Serial private static final long serialVersionUID = 6000054069559657434L;

  public Disconnection() {}
}
