package sn.lhacksrt.maeliaserver.paramcatalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterAdminUseCase;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.out.ParameterCatalogRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParameterAdminService implements ParameterAdminUseCase {

    private final ParameterCatalogRepository repository;

    public ParameterAdminService(ParameterCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public ParameterSpec create(ParameterSpecUpsertRequest req) {
        if (repository.existsSpec(req.gamlName())) {
            throw new IllegalArgumentException("Un paramètre existe déjà avec le nom : " + req.gamlName());
        }
        validateGroup(req.group());
        return repository.saveSpec(toSpec(req.gamlName(), req));
    }

    @Override
    public ParameterSpec update(String gamlName, ParameterSpecUpsertRequest req) {
        if (!repository.existsSpec(gamlName)) {
            throw new IllegalArgumentException("Paramètre introuvable : " + gamlName);
        }
        validateGroup(req.group());
        // gamlName (identifiant) immuable : on conserve celui du chemin.
        return repository.saveSpec(toSpec(gamlName, req));
    }

    @Override
    public void delete(String gamlName) {
        if (!repository.existsSpec(gamlName)) {
            throw new IllegalArgumentException("Paramètre introuvable : " + gamlName);
        }
        repository.deleteSpec(gamlName);
    }

    private void validateGroup(String group) {
        Set<String> groups = repository.findAllGroups().stream()
                .map(g -> g.id()).collect(Collectors.toSet());
        if (!groups.contains(group)) {
            throw new IllegalArgumentException("Groupe inconnu : " + group);
        }
    }

    private ParameterSpec toSpec(String gamlName, ParameterSpecUpsertRequest req) {
        List<String> allowed = (req.allowedValues() == null || req.allowedValues().isEmpty())
                ? null : req.allowedValues();
        return new ParameterSpec(
                gamlName,
                req.label(),
                req.group(),
                ParamType.fromString(req.type()),
                blankToNull(req.defaultValue()),
                blankToNull(req.unit()),
                allowed,
                blankToNull(req.visibleIf()),
                blankToNull(req.enabledIf()),
                blankToNull(req.optionsDataSpec()),
                blankToNull(req.optionsColumn()),
                blankToNull(req.optionsSource()),
                req.advanced(),
                req.order());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
