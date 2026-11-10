package org.prolegacy.atom2027.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.instancio.Select;
import org.jspecify.annotations.NonNull;
import org.prolegacy.atom2027.common.health.HealthValidator;
import org.prolegacy.atom2027.common.hibernate.JdbcTemplateFactory;
import org.prolegacy.atom2027.common.hibernate.JsonRowMapper;
import org.prolegacy.atom2027.common.query.query.JPAQueryFactory;
import org.prolegacy.atom2027.second_datasource.entities.DummyEntityB;
import org.prolegacy.atom2027.second_datasource.entities.QDummyEntityB;
import org.prolegacy.atom2027.second_datasource.entities.ids.DummyEntityBId;
import org.prolegacy.atom2027.second_datasource.repositories.DummyEntityBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SecondaryConnectionValidator implements HealthValidator {
    @Autowired
    @Lazy
    private SecondaryConnectionValidator self;

    @Autowired
    private DummyEntityBRepository dummyEntityBRepository;
    @Autowired
    @Qualifier("secondaryJPAQueryFactory")
    private JPAQueryFactory queryFactory;
    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcTemplate secondaryJdbcTemplate;
    @Autowired
    private JdbcTemplateFactory jdbcTemplateFactory;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.dynamic-datasources.secondary.url}")
    private String datasourceUrl;
    @Value("${app.dynamic-datasources.secondary.username}")
    private String datasourceUsername;
    @Value("${app.dynamic-datasources.secondary.password}")
    private String datasourcePassword;
//    @Value("${app.dynamic-datasources.secondary.driver-class-name}")
//    private String datasourceDriver;

    @Override
    public String name() {
        return "SecondaryConnectionValidator";
    }

    @Override
    public ValidationResult validate() {
        final List<DummyEntityB> refList = self.populateData();

        List<DummyEntityB> resList = queryFactory.from(QDummyEntityB.dummyEntityB).orderBy(QDummyEntityB.dummyEntityB.id.asc()).selectDto(DummyEntityB.class).fetch();
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Secondary datasource doesn't return the expected value");
        }
        resList = this.secondaryJdbcTemplate.query("select * from dummy_entityb order by id", (rs, _) -> new DummyEntityB(new DummyEntityBId(rs.getLong("id")), rs.getString("dummy_string"), rs.getLong("dummy_long")));
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Secondary jdbcTemplate doesn't return the expected value");
        }

        final JdbcTemplate jdbcTemplate = jdbcTemplateFactory.jdbcTemplate(new JdbcTemplateFactory.ConnectionParams(
                this.datasourceUrl,
                this.datasourceUsername,
                this.datasourcePassword,
                null
        ));

        resList = jdbcTemplate.query("select * from dummy_entityb order by id", new JsonRowMapper<>(DummyEntityB.class, objectMapper));
        if (!refList.equals(resList)) {
            return ValidationResult.fail("Dynamic jdbcTemplate doesn't return the expected value");
        }

        return ValidationResult.ok();
    }

    @Transactional(transactionManager = "secondaryTransactionManager")
    protected @NonNull List<DummyEntityB> populateData() {
        final boolean hasData = !dummyEntityBRepository.findAll().isEmpty();

        InstancioApi<DummyEntityB> dummyEntityBInstancioApi = Instancio.of(DummyEntityB.class)
                .generate(Select.field(DummyEntityB::getDummyString), gen -> gen.string().mixedCase());
        if (hasData) {
            dummyEntityBInstancioApi = dummyEntityBInstancioApi.generate(Select.field(DummyEntityB::getId), generators -> generators.longSeq().as(DummyEntityBId::new));
        } else {
            dummyEntityBInstancioApi = dummyEntityBInstancioApi.ignore(Select.field(DummyEntityB::getId));
        }
        final List<DummyEntityB> refList = dummyEntityBRepository.saveAll(Instancio.ofList(dummyEntityBInstancioApi.toModel()).size(15).create());
        return refList;
    }
}
