package org.dspace.identifier;

import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.content.logic.Filter;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * This abstract class adds extra method signatures so that implementing IdentifierProviders can
 * handle "skip filter" booleans, so that any configured filters can be skipped and DOI registration forced.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public abstract class FilteredIdentifierProvider extends IdentifierProvider {

    protected Filter filterService;

    public FilteredIdentifierProvider() {

    }

    @Autowired
    public FilteredIdentifierProvider(Filter filterService) {
        this.filterService = filterService;
    }

    @Autowired
    public void setFilterService(Filter filterService) {
        this.filterService = filterService;
    }

    @Autowired
    public Filter getFilterService() {
        return this.filterService;
    }

    public abstract String register(Context context, DSpaceObject dso, Boolean skipFilter)
        throws IdentifierException;

    public abstract void register(Context context, DSpaceObject dso, String identifier, Boolean skipFilter)
        throws IdentifierException;

    public abstract void reserve(Context context, DSpaceObject dso, String identifier, Boolean skipFilter)
        throws IdentifierException, IllegalArgumentException, SQLException;

    public abstract String mint(Context context, DSpaceObject dso, Boolean skipFilter) throws IdentifierException;

    public abstract Boolean canMint(Context context, DSpaceObject dso) throws IdentifierException;


}