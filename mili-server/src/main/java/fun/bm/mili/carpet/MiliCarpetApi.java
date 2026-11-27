package fun.bm.mili.carpet;

import fun.bm.mili.api.carpet.CarpetApi;
import org.leavesmc.leaves.protocol.CarpetServerProtocol;

import java.util.Comparator;
import java.util.List;

/** Read-only view of Mili's Carpet rule protocol snapshots. */
public final class MiliCarpetApi implements CarpetApi {
    public static final CarpetApi INSTANCE = new MiliCarpetApi();

    private MiliCarpetApi() {
    }

    @Override
    public List<Rule> rules() {
        return CarpetServerProtocol.CarpetRules.snapshot().stream()
            .map(rule -> new Rule(rule.identifier(), rule.name(), rule.value()))
            .sorted(Comparator.comparing(Rule::name))
            .toList();
    }
}
