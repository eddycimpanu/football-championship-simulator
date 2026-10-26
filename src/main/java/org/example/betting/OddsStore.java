package org.example.betting;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Where the odds are kept. The betting code only knows this interface; the database code implements it.
public interface OddsStore {

    Map<Integer, MatchOdds> findByMatchIds(Collection<Integer> matchIds);

    void saveAll(List<MatchOdds> odds);
}
