package org.prolegacy.atom2027.health;

import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Select;
import org.prolegacy.atom2027.common.health.HealthValidator;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.second_datasource.entities.DummyEntityB;
import org.prolegacy.atom2027.second_datasource.entities.QDummyEntityB;
import org.prolegacy.atom2027.second_datasource.entities.ids.DummyEntityBId;
import org.prolegacy.atom2027.second_datasource.repositories.DummyEntityBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SecondaryConnectionValidator implements HealthValidator {
    @Autowired
    private DummyEntityBRepository dummyEntityBRepository;
    @Autowired
    @Qualifier("secondaryJPAQueryFactory")
    private JPAQueryFactory queryFactory;

    @Override
    public String name() {
        return "SecondaryConnectionValidator";
    }

    @Override
    @Transactional(transactionManager = "secondaryTransactionManager")
    public ValidationResult validate() {
        final boolean hasData = !dummyEntityBRepository.findAll().isEmpty();

        InstancioApi<DummyEntityB> dummyEntityBInstancioApi = Instancio.of(DummyEntityB.class)
                .generate(Select.field(DummyEntityB::getDummyString), gen -> gen.string().mixedCase());
        if (hasData) {
            dummyEntityBInstancioApi = dummyEntityBInstancioApi.generate(Select.field(DummyEntityB::getId), generators -> generators.longSeq().as(DummyEntityBId::new));
        } else {
            dummyEntityBInstancioApi = dummyEntityBInstancioApi.ignore(Select.field(DummyEntityB::getId));
        }
        final List<DummyEntityB> refList = dummyEntityBRepository.saveAll(Instancio.ofList(dummyEntityBInstancioApi.toModel()).size(15).create());

        final List<DummyEntityB> resList = queryFactory.from(QDummyEntityB.dummyEntityB).orderBy(QDummyEntityB.dummyEntityB.id.asc()).selectDto(DummyEntityB.class).fetch();
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Secondary datasource doesn't return the expected value");
        }

        return ValidationResult.ok();
    }
}
