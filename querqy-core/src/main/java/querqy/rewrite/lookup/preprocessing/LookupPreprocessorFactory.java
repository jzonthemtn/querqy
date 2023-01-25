package querqy.rewrite.lookup.preprocessing;

public class LookupPreprocessorFactory {

    private static final LookupPreprocessor IDENTITY_PREPROCESSOR = charSequence -> charSequence;

    private static final LookupPreprocessor GERMAN_PREPROCESSOR = PipelinePreprocessor.of(
            LowerCasePreprocessor.create(),
            GermanUmlautPreprocessor.create(),
            GermanNounNormalizer.create()
    );

    public static LookupPreprocessor fromType(final LookupPreprocessorType type) {

        switch (type) {
            case NONE:
                return IDENTITY_PREPROCESSOR;

            case GERMAN:
                return GERMAN_PREPROCESSOR;

            default:
                throw new IllegalStateException("Preprocessor of type " + " is currently not supported");
        }

    }
}