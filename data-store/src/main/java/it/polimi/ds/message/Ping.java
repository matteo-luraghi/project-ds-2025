package it.polimi.ds.message;

import java.io.Serial;
import java.io.Serializable;

/**
 * Ping
 *
 * <p>used to keep alive the socket connection
 */
public class Ping implements Serializable {
  @Serial private static final long serialVersionUID = -1366111626343826190L;

  public Ping() {}
}
