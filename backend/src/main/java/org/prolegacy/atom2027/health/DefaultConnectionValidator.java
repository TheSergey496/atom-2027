package org.prolegacy.atom2027.health;

import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Select;
import org.prolegacy.atom2027.common.health.HealthValidator;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.entities.DummyEntityA;
import org.prolegacy.atom2027.entities.QDummyEntityA;
import org.prolegacy.atom2027.entities.ids.DummyEntityAId;
import org.prolegacy.atom2027.repositories.DummyEntityARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DefaultConnectionValidator implements HealthValidator {
    @Autowired
    private DummyEntityARepository dummyEntityARepository;
    @Autowired
    private JPAQueryFactory queryFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String name() {
        return "DefaultConnectionValidator";
    }

    @Override
    @Transactional
    public ValidationResult validate() {
        final boolean hasData = !dummyEntityARepository.findAll().isEmpty();

        InstancioApi<DummyEntityA> dummyEntityAInstancioApi = Instancio.of(DummyEntityA.class)
                .generate(Select.field(DummyEntityA::getDummyString), gen -> gen.string().mixedCase());
        if (hasData) {
            dummyEntityAInstancioApi = dummyEntityAInstancioApi.generate(Select.field(DummyEntityA::getId), generators -> generators.longSeq().as(DummyEntityAId::new));
        } else {
            dummyEntityAInstancioApi = dummyEntityAInstancioApi.ignore(Select.field(DummyEntityA::getId));
        }
        final List<DummyEntityA> refList = dummyEntityARepository.saveAll(Instancio.ofList(dummyEntityAInstancioApi.toModel()).size(15).create());

        List<DummyEntityA> resList = queryFactory.from(QDummyEntityA.dummyEntityA).orderBy(QDummyEntityA.dummyEntityA.id.asc()).selectDto(DummyEntityA.class).fetch();
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Default datasource doesn't return the expected value");
        }

        resList = jdbcTemplate.query("select * from dummy_entitya order by id", (rs, _) -> new DummyEntityA(new DummyEntityAId(rs.getLong("id")), rs.getString("dummy_string"), rs.getLong("dummy_long")));
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Default jdbcTemplate doesn't return the expected value");
        }
        return ValidationResult.ok();
    }
}
