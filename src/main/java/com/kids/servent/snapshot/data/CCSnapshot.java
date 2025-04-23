package com.kids.servent.snapshot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class CCSnapshot implements Serializable {

    private final int serventId;
    private final int amount;
}
