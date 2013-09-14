# Mira implementation
import util
PRINT = True

class MiraClassifier:
  """
  Mira classifier.
  
  Note that the variable 'datum' in this code refers to a counter of features
  (not to a raw samples.Datum).
  """
  def __init__( self, legalLabels, max_iterations):
    self.legalLabels = legalLabels
    self.type = "mira"
    self.automaticTuning = False 
    self.C = 0.001
    self.legalLabels = legalLabels
    self.max_iterations = max_iterations
    self.initializeWeightsToZero()

  def initializeWeightsToZero(self):
    "Resets the weights of each label to zero vectors" 
    self.weights = {}
    for label in self.legalLabels:
      self.weights[label] = util.Counter() # this is the data-structure you should use
  
  def train(self, trainingData, trainingLabels, validationData, validationLabels):
    "Outside shell to call your method. Do not modify this method."  
      
    self.features = trainingData[0].keys() # this could be useful for your code later...
    
    if (self.automaticTuning):
        Cgrid = [0.002, 0.004, 0.008]
    else:
        Cgrid = [self.C]
        
    return self.trainAndTune(trainingData, trainingLabels, validationData, validationLabels, Cgrid)

  def trainAndTune(self, trainingData, trainingLabels, validationData, validationLabels, Cgrid):
    """
    This method sets self.weights using MIRA.  Train the classifier for each value of C in Cgrid, 
    then store the weights that give the best accuracy on the validationData.
    
    Use the provided self.weights[label] data structure so that 
    the classify method works correctly. Also, recall that a
    datum is a counter from features to values for those features
    representing a vector of values.
    """
    "*** YOUR CODE HERE ***"
    bestWeight = None
    min_error = 999
    trainingNum = len(trainingData)
    for c in Cgrid:
      # find whether this c is okay - start with original weights
      weights = self.weights

      for iteration in range(self.max_iterations):
	for i in range(trainingNum):
	  f = trainingData[i] # f[feature] = 0 or 1
	  scores = util.Counter()

	  # calculate f * w_y for each y
	  for label in self.legalLabels:
	    scores[label] = f * self.weights[label]
	  
	  predictLabel = max(scores.keys(), key=lambda x: scores[x])
	  correctLabel = trainingLabels[i]

	  # predictLabel should be exactly traininglabels[i], if not, adjust it
          if predictLabel != correctLabel:
	    tempC = 1.0 * ((weights[predictLabel] - weights[correctLabel]) * f + 1) / (f * f) / 2 

	    t = min(c, tempC)
	    t_f = util.Counter()
	    for key, value in f.items():
	      t_f[key] = t*f[key]

	    weights[predictLabel] = weights[predictLabel] - t_f
	    weights[correctLabel] = weights[correctLabel] + t_f

      # test on validation set
      guess = self.classify(validationData)
      errors = sum([1 for i in range(len(validationData)) if validationLabels[i] != guess[i]])
      # update this.k if current k is better
      if errors < min_error:
	min_error = errors
	bestWeight = weights

    self.weights = bestWeight

  def classify(self, data ):
    """
    Classifies each datum as the label that most closely matches the prototype vector
    for that label.  See the project description for details.
    
    Recall that a datum is a util.counter... 
    """
    guesses = []
    for datum in data:
      vectors = util.Counter()
      for l in self.legalLabels:
        vectors[l] = self.weights[l] * datum
      guesses.append(vectors.argMax())
    return guesses

  
  def findHighOddsFeatures(self, label1, label2):
    """
    Returns a list of the 100 features with the greatest difference in feature values
                     w_label1 - w_label2

    """
    featuresOdds = []

    "*** YOUR CODE HERE ***"

    return featuresOdds

