package org.prolegacy.atom2027.init;


import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.util.InitializationOrder;
import org.prolegacy.atom2027.second_datasource.repositories.ForeignEdificeRepository;
import org.prolegacy.atom2027.services.DataStructureLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(InitializationOrder.DEFAULT_USER_INIT + 1)
public class DataStructureLoad implements ApplicationRunner {
    @Autowired
    private DataStructureLoader dataStructureLoader;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        dataStructureLoader.reloadData();
    }
}
