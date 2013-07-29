package org.ccci.maintenance;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * @author Matt Drees
 */
public class PathParserTest {


    PathParser parser;

    @BeforeMethod
    public void buildParser()
    {
        parser = new PathParser();
    }

    @Test
    public void testParsingValidDefaultFilterPath()
    {
        parser.parse("/updateWindow");
        assert parser.isValid();
    }

    @Test
    public void testParsingValidDefaultFilterPathGivesNullName()
    {
        parser.parse("/updateWindow");
        assertThat(parser.getFilterName(), is(nullValue()));
    }

    @Test
    public void testParsingValidDefaultFilterPathGivesCorrectAction()
    {
        parser.parse("/updateWindow");
        assertThat(parser.getAction(), is("updateWindow"));
    }

    @Test
    public void testParsingValidNamedFilterPath()
    {
        parser.parse("/special/updateWindow");
        assert parser.isValid();
    }

    @Test
    public void testParsingValidNamedFilterPathGivesCorrectName()
    {
        parser.parse("/special/updateWindow");
        assertThat(parser.getFilterName(), is("special"));
    }

    @Test
    public void testParsingValidNamedFilterPathGivesCorrectAction()
    {
        parser.parse("/special/updateWindow");
        assertThat(parser.getAction(), is("updateWindow"));
    }

    @Test
    public void testParsingInvalidSlashPath()
    {
        parser.parse("/");
        assert !parser.isValid();
    }

    @Test
    public void testParsingInvalidEmptyPath()
    {
        parser.parse("");
        assert !parser.isValid();
    }

    @Test
    public void testParsingInvalidNullPath()
    {
        parser.parse(null);
        assert !parser.isValid();
    }

    @Test
    public void testParsingInvalidLongPath()
    {
        parser.parse("/foo/bar/baz");
        assert !parser.isValid();
    }
}
