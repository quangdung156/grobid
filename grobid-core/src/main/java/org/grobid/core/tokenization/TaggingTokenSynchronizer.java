package org.grobid.core.tokenization;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zholudev on 11/01/16.
 * Synchronize tagging result and layout tokens
 */
public class TaggingTokenSynchronizer implements Iterator<LabeledTokensContainer>, Iterable<LabeledTokensContainer> {
    private final GrobidModels grobidModel;
    private final Iterator<Triple<String, String, String>> tokensAndLabelsIt;
    private final Iterator<LayoutToken> tokenizationsIt;
    private int tokensAndLabelsPtr;
    private int tokenizationsPtr;

    public TaggingTokenSynchronizer(GrobidModels grobidModel, String result, List<LayoutToken> tokenizations) {
        this(grobidModel, result, tokenizations, false);
    }

    public TaggingTokenSynchronizer(GrobidModels grobidModel, String result, List<LayoutToken> tokenizations,
                                    boolean addFeatureStrings) {
        this.grobidModel = grobidModel;
        List<Triple<String, String, String>> tokensAndLabels = GenericTaggerUtils.getTokensWithLabelsAndFeatures(result, addFeatureStrings);
        tokensAndLabelsIt = tokensAndLabels.iterator();
        tokenizationsIt = tokenizations.iterator();
    }

    @Override
    public boolean hasNext() {
        return tokensAndLabelsIt.hasNext();
    }

    @Override
    //null value indicates an empty line in a tagging result
    public LabeledTokensContainer next() {
        Triple<String, String, String> p = tokensAndLabelsIt.next();

        if (p == null) {
            return null;
        }

        String resultToken = p.getA();
        String label = p.getB();
        String featureString = p.getC();

        List<LayoutToken> layoutTokenBuffer = new ArrayList<>();
        boolean stop = false;
        boolean addSpace = false;
        boolean newLine = false;
        while ((!stop) && (tokenizationsIt.hasNext())) {
            LayoutToken layoutToken = tokenizationsIt.next();

            layoutTokenBuffer.add(layoutToken);
            String tokOriginal = layoutToken.t();
            if (LayoutTokensUtil.spaceyToken(tokOriginal)) {
                addSpace = true;
            } else if (LayoutTokensUtil.newLineToken(tokOriginal)) {
                newLine = true;
            } else if (tokOriginal.equals(resultToken)) {
                stop = true;
            } else {
                throw new IllegalStateException("IMPLEMENTATION ERROR: " +
                        "tokens (at pos: " + tokensAndLabelsPtr + ")got dissynchronized with tokenizations (at pos: "
                        + tokenizationsPtr + " )");
            }
            tokenizationsPtr++;
        }

        resultToken = LayoutTokensUtil.removeSpecialVariables(resultToken);

        tokensAndLabelsPtr++;
        LabeledTokensContainer labeledTokensContainer =
                new LabeledTokensContainer(layoutTokenBuffer, resultToken, TaggingLabel.getLabel(grobidModel, label),
                GenericTaggerUtils.isBeginningOfEntity(label));

        labeledTokensContainer.setFeatureString(featureString);

        labeledTokensContainer.setSpacePreceding(addSpace);
        labeledTokensContainer.setNewLinePreceding(newLine);

        return labeledTokensContainer;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<LabeledTokensContainer> iterator() {
        return this;
    }
}
