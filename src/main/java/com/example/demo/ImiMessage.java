package com.example.demo;

import java.io.Serializable;
import java.sql.Timestamp;

public record ImiMessage(Timestamp ts, String srcApp, String dstApp, byte[] payload) implements Serializable {

}
