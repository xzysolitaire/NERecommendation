NERecommendation
================
Name entity recommender system based on the co-occurrence relevance of name entities.

Tools we use
================
1. Freebase API: https://developers.google.com/freebase/
2. Stanford Name Entity Recognizer: http://nlp.stanford.edu/software/CRF-NER.shtml

Ideas and Implementations
================
Basic Assumption:
We assume that two name entities have correlation with each other if they appear in the same corpus.
The test data we use is 5000 articles of sport news crawled from the ESPN website. 

Problems we solve:
1. In-document name entity resolution is based on the textual feature.
2. Cross-document name entitiy resolution is implemented with Freebase API. 

Improvements:
For ambiguous queries, the inverted map for terms in NEs will provide relevant query recommendation. 


