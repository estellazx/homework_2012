import util
import classificationMethod
import math

class NaiveBayesClassifier(classificationMethod.ClassificationMethod):
  """
  See the project description for the specifications of the Naive Bayes classifier.
  
  Note that the variable 'datum' in this code refers to a counter of features
  (not to a raw samples.Datum).
  """
  def __init__(self, legalLabels):
    self.legalLabels = legalLabels
    self.type = "naivebayes"
    self.k = 1 # this is the smoothing parameter, ** use it in your train method **
    self.automaticTuning = False # Look at this flag to decide whether to choose k automatically ** use this in your train method **
    
  def setSmoothing(self, k):
    """
    This is used by the main method to change the smoothing parameter before training.
    Do not modify this method.
    """
    self.k = k

  def train(self, trainingData, trainingLabels, validationData, validationLabels):
    """
    Outside shell to call your method. Do not modify this method.
    """  
      
    self.features = trainingData[0].keys() # this could be useful for your code later...
    
    if (self.automaticTuning):
        kgrid = [0.001, 0.01, 0.05, 0.1, 0.5, 1, 5, 10, 20, 50]
    else:
        kgrid = [self.k]
        
    self.trainAndTune(trainingData, trainingLabels, validationData, validationLabels, kgrid)
      
  def trainAndTune(self, trainingData, trainingLabels, validationData, validationLabels, kgrid):
    """
    Trains the classifier by collecting counts over the training data, and
    stores the Laplace smoothed estimates so that they can be used to classify.
    Evaluate each value of k in kgrid to choose the smoothing parameter 
    that gives the best accuracy on the held-out validationData.
    
    trainingData and validationData are lists of feature Counters.  The corresponding
    label lists contain the correct label for each datum.
    
    To get the list of all possible features or labels, use self.features and 
    self.legalLabels.
    """

    "*** YOUR CODE HERE ***"
    min_error = len(validationData)
    best_p = None
    best_p_f = None

    for k in kgrid:
      self.c = util.Counter()
      self.c_f = util.Counter()
      self.p = util.Counter()
      self.p_f = util.Counter()

      #calculate prior of the lebal
      for i in range(len(trainingData)):
        digit = trainingLabels[i]
	self.c[digit]+=1
      #calculate c(f, Y) in training data
      for i in range(len(trainingData)):
        digit = trainingLabels[i]
	for pixel, value in trainingData[i].items():
	  # if value of pixel == 1, then add 1; else add 0
	  self.c_f[pixel, digit] += value

      # probability P(Y)
      for y in self.legalLabels:
        self.p[y] = 1.0 * self.c[y]/len(trainingData)
      # probability P(f|Y)
      for y in self.legalLabels:
        for f in self.features:
          self.p_f[f, y] = 1.0 * (self.c_f[f, y] + k) / (self.c[y] + k * 2)

      # test its performance on validation data
      guess = self.classify(validationData)
      errors = sum([1 for i in range(len(validationData)) if validationLabels[i] != guess[i]])
      # update this.k if current k is better
      if errors < min_error:
        min_error = errors
	best_p = self.p
	best_p_f = self.p_f

    self.p = best_p
    self.p_f = best_p_f
    #util.raiseNotDefined()

  def classify(self, testData):
    """
    Classify the data based on the posterior distribution over labels.
    
    You shouldn't modify this method.
    """
    guesses = []
    self.posteriors = [] # Log posteriors are stored for later data analysis (autograder).
    for datum in testData:
      posterior = self.calculateLogJointProbabilities(datum)
      guesses.append(posterior.argMax())
      self.posteriors.append(posterior)
    return guesses
      
  def calculateLogJointProbabilities(self, datum):
    """
    Returns the log-joint distribution over legal labels and the datum.
    Each log-probability should be stored in the log-joint counter, e.g.    
    logJoint[3] = <Estimate of log( P(Label = 3, datum) )>
    """
    logJoint = util.Counter()
    
    "*** YOUR CODE HERE ***"
    #util.raiseNotDefined()
    for y in self.legalLabels:
      s = math.log(self.p[y])
      for pixel, value in datum.items():
        if value == 1:
          s += math.log(self.p_f[pixel, y])
	else:
          s += math.log(1 - self.p_f[pixel, y])
      
      logJoint[y] = s

    return logJoint
  
  def findHighOddsFeatures(self, label1, label2):
    """
    Returns the 100 best features for the odds ratio:
            P(feature=1 | label1)/P(feature=1 | label2) 
    """
    "*** YOUR CODE HERE ***"
    odds = util.Counter() # odds[feature] = P(feature=1 | label1)/P(feature=1 | label2) 
        
    for feature in self.features:
      odds[feature] = self.p_f[feature, label1] / self.p_f[feature, label2]

    features = self.features
    # sort features according to their odds values, decreasingly
    features.sort(lambda x, y: -cmp(odds[x], odds[y]))

    featuresOdds = features[:100]
    return featuresOdds
    

    
      
