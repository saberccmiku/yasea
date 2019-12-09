package net.ossrs.yasea.demo.net;

import net.ossrs.yasea.demo.bean.equipment.Equipment;

import io.reactivex.Observable;


public interface TestService {
    Observable<Equipment> getInfo();
}
