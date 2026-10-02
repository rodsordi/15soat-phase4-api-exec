package br.com.fiap.phase4.exec.architecture;

import br.com.fiap.phase4.commons.test.architecture.CommonArchitectureRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "br.com.fiap.phase4.exec", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchTests commonRules = ArchTests.in(CommonArchitectureRules.class);
}
