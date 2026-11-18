package org.prolegacy.atom2027.controllers;

import org.prolegacy.atom2027.common.annotation.TypescriptEndpoint;
import org.prolegacy.atom2027.services.BrigadeOptimizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@TypescriptEndpoint
@RequestMapping("brigades")
@CrossOrigin("*")
public class BrigadeOptimizerController {
    @Autowired
    private BrigadeOptimizerService brigadeOptimizerService;

    @PostMapping("optimize")
    public void optimizeBrigades() {
        this.brigadeOptimizerService.optimize();
    }
}
