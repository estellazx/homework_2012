# agents.py
# -----------------
# Licensing Information: Please do not distribute or publish solutions to this
# project. You are free to use and extend these projects for educational
# purposes. The Pacman AI projects were developed at UC Berkeley, primarily by
# John DeNero (denero@cs.berkeley.edu) and Dan Klein (klein@cs.berkeley.edu).
# For more info, see http://inst.eecs.berkeley.edu/~cs188/sp09/pacman.html

from captureAgents import CaptureAgent
from captureAgents import AgentFactory
import distanceCalculator
import random, time, util, math
from game import Directions, Actions
import keyboardAgents
import game
from util import nearestPoint
from pprint import pprint

# global variables
beliefs = util.Counter() # beliefs of position of enemies - all the emenies share the same counter
invaded = False

#############
# FACTORIES #
#############

NUM_KEYBOARD_AGENTS = 0
class Mayonnaise(AgentFactory):
  "Returns one keyboard agent and offensive reflex agents"

  def __init__(self, isRed, first='offense', second='defense', rest='offense'):
    AgentFactory.__init__(self, isRed)
    self.agents = [first, second]
    self.rest = rest

  def getAgent(self, index):
    if len(self.agents) > 0:
      return self.choose(self.agents.pop(0), index)
    else:
      return self.choose(self.rest, index)

  def choose(self, agentStr, index):
    if agentStr == 'keys':
      global NUM_KEYBOARD_AGENTS
      NUM_KEYBOARD_AGENTS += 1
      if NUM_KEYBOARD_AGENTS == 1:
        return keyboardAgents.KeyboardAgent(index)
      elif NUM_KEYBOARD_AGENTS == 2:
        return keyboardAgents.KeyboardAgent2(index)
      else:
        raise Exception('Max of two keyboard agents supported')
    elif agentStr == 'offense':
      return OffensiveReflexAgent(index)
    elif agentStr == 'defense':
      return DefensiveReflexAgent(index)
    else:
      raise Exception("No staff agent identified by " + agentStr)


class AllOffenseAgents(AgentFactory):
  "Returns one keyboard agent and offensive reflex agents"

  def __init__(self, **args):
    AgentFactory.__init__(self, **args)

  def getAgent(self, index):
    return OffensiveReflexAgent(index)

class OffenseDefenseAgents(AgentFactory):
  "Returns one keyboard agent and offensive reflex agents"

  def __init__(self, **args):
    AgentFactory.__init__(self, **args)
    self.offense = False

  def getAgent(self, index):
    self.offense = not self.offense
    if self.offense:
      return OffensiveReflexAgent(index)
    else:
      return DefensiveReflexAgent(index)

##########
# Agents #
##########

class ReflexCaptureAgent(CaptureAgent):
  """
  A base class for reflex agents that chooses score-maximizing actions
  """

  def chooseAction(self, gameState):
    """
    Picks among the actions with the highest Q(s,a).
    """
    actions = gameState.getLegalActions(self.index)

    # this is about DBN. We'll update the beliefs according to our combined efforts
    # see what's happened just now
    self.elapseTime(gameState)
    # all agents on our side are responsible for update beliefs according to what they see
    self.observe(gameState)

    # You can profile your evaluation time by uncommenting these lines
    # start = time.time()
    values = [self.evaluate(gameState, a) for a in actions]
    # print 'eval time for agent %d: %.4f' % (self.index, time.time() - start)

    maxValue = max(values)
    bestActions = [a for a, v in zip(actions, values) if v == maxValue]

    return random.choice(bestActions)

  def registerInitialState(self, gameState):
    """
    Initialize the beliefs of the enemies uniformly
    and many related operations
    """
    CaptureAgent.registerInitialState(self, gameState)

    global beliefs

    # legalPositions is a useful variable. it's not provided! let's construct it
    width = gameState.data.layout.width
    height = gameState.data.layout.height
    self.legalPositions = [(x, y) for x in range(width) for y in range(height) if not gameState.hasWall(x, y)]

    # set legalPositions for defending and offending respectively
    self.defensiveLegalPositions = [p for p in self.legalPositions if self.isOurTerrain(gameState, p)]
    self.offensiveLegalPositions = [p for p in self.legalPositions if not (p in self.defensiveLegalPositions)]

    # we need a new distancer for calculating distance within defensive area
    tmpGameState = gameState.deepCopy()
    defensiveLayout = tmpGameState.data.layout
    for pos in self.offensiveLegalPositions:
      defensiveLayout.walls[pos[0]][pos[1]] = True
    self.defensiveDistancer = distanceCalculator.Distancer(defensiveLayout)
    self.defensiveDistancer.getMazeDistances()

    # initialize beliefs according to legalPositions
    for enemyId in self.getOpponents(gameState):
      self.initializeBeliefs(gameState, enemyId, initial = True)

    # set availableActions for each grid
    self.setNeighbors(gameState)

    # set boundaries - entries of the enemy!
    self.setBoundary(gameState)

    # set trainsition model
    self.transition = util.Counter()
    self.transition['favor'] = 0.8
    self.transition['indifferent'] = 0.05
    
  def initializeBeliefs(self, gameState, enemyId, initial = False):
    """
    if initial, we will put the belief at the start point
    """
    global beliefs
    beliefs[enemyId] = util.Counter() 
    if initial:
      beliefs[enemyId][gameState.getInitialAgentPosition(enemyId)] = 1
    else:
      for pos in self.offensiveLegalPositions:
	beliefs[enemyId][pos] = 1
	beliefs[enemyId].normalize()
    
  def observe(self, gameState):
    """
    for DBN. change the beliefs after observation
    this should be called by each agent on our side
    """
    global beliefs
    myPos = gameState.getAgentState(self.index).getPosition()
    allNoiseDistances = gameState.getAgentDistances() # this is for all; obviously, we don't need our own "noisy" positions

    for i in self.getOpponents(gameState):
      enemy = gameState.getAgentState(i)
      pos = enemy.getPosition()
      if pos != None:
        # 1st way: there's REALLY an enemy here
	beliefs[i] = util.Counter()
        beliefs[i][pos] = 1
      else:
        # 2nd way: SONAR
	noiseDistance = allNoiseDistances[i]
	for p in self.legalPositions:
	  trueDistance = util.manhattanDistance(p, myPos)
	  if trueDistance < 5:
	    # this kind of observation can be got in 2nd way
	    beliefs[i][p] = 0
	  else:
	    beliefs[i][p] *= gameState.getDistanceProb(trueDistance, noiseDistance)

    # 3rd way: infered from food
    foodNow = self.getFoodYouAreDefending(gameState).asList()
    for pos in foodNow:
      for i in self.getOpponents(gameState):
        beliefs[i][pos] = 0 # well, food is there - no enemy there

    previousState = self.getPreviousObservation()
    if previousState != None:
      # when this is not the initial state
      foodPrevious = self.getFoodYouAreDefending(previousState).asList()
      difference = [p for p in self.legalPositions if (p in foodNow) != (p in foodPrevious)]
      # missing dot on our terrain?! I SEE YOU, invader!
      for pos in difference:
        # we need to check which enemy eat that dot
	# we use expectation of the enemy position, attribute this to the nearest enemy
	trueAgent = min([i for i in self.getOpponents(gameState)], key=lambda i: self.getMazeDistance(pos, self.mostLikelyPosition(gameState, self.defensiveLegalPositions, i)[0]))
	beliefs[trueAgent] = util.Counter()
	beliefs[trueAgent][pos] = 1

    for i in self.getOpponents(gameState):
      beliefs[i].normalize()

      # in case some of enemy is missed
      if sum([value for value in beliefs[i].values()]) == 0:
        # redistribute
        self.initializeBeliefs(gameState, i)

  def elapseTime(self, gameState):
    """
    for DBN. change the beliefs after one round of actions for all agent
    this should be called by only the first agent on our side
    """
    global beliefs
    newBelief = util.Counter() # P(G_t|N_1..t-1)
    
    # now, according to our transition model, redistribute these probabilities
    # i is the id of agent we want to update
    i = self.index - 1
    if i == -1: i = gameState.getNumAgents() - 1

    for nowPosition in self.legalPositions:
      if self.isOurTerrain(gameState, nowPosition):
	# this pacman will choose the neighbor which closest to a food
	findMin = self.findNearestPathToFood
      else:
	# this ghost will choose the neighbor which closest to our pacman
	findMin = self.findNearestPathToPacman

      myPos = gameState.getAgentPosition(self.index)
      minNeighbor = findMin(gameState, self.neighbors[nowPosition])
      # for each neighbor of nowPosition, find its probability after transition
      for neighbor in self.neighbors[nowPosition]:
	if neighbor != minNeighbor:
	  # it's just a common neighbor
	  newBelief[neighbor] += self.transition['indifferent'] * beliefs[i][nowPosition]
	else:
	  # for most likely direction, put more weight on it
	  newBelief[neighbor] += self.transition['favor'] * beliefs[i][nowPosition]

    newBelief.normalize()

    # update the beliefs with new time slice
    beliefs[i] = newBelief

    # this is used for drawing distribution
    # self.displayDistributionsOverPositions(gameState, beliefs)

  def displayDistributionsOverPositions(self, gameState, distributions):
    dists = []
    for i in range(gameState.getNumAgents()):
      if i in distributions.keys():
        dists.append(distributions[i])
      else:
        dists.append(util.Counter())
    self.display.updateDistributions(dists)

  def findNearestPathToPacman(self, gameState, positions):
    pacmanPos = gameState.getAgentPosition(self.index)
    distanceFunc = lambda position : self.getMazeDistance(pacmanPos, position)
    return min(positions, distanceFunc)

  def findNearestPathToFood(self, gameState, positions):
    foodList = self.getFoodYouAreDefending(gameState).asList()
    minDis = 99999
    minPos = None
    for pos in positions:
      for food in foodList:
        dis = self.getMazeDistance(pos, food)
	if dis < minDis:
	  minDis = dis
	  minPos = pos

    return minPos

  def getSuccessor(self, gameState, action):
    """
    Finds the next successor which is a grid position (location tuple).
    """
    successor = gameState.generateSuccessor(self.index, action)
    pos = successor.getAgentState(self.index).getPosition()
    if pos != nearestPoint(pos):
      # Only half a grid position was covered
      return successor.generateSuccessor(self.index, action)
    else:
      return successor

  def evaluate(self, gameState, action):
    """
    Computes a linear combination of features and feature weights
    """
    features = self.getFeatures(gameState, action)
    weights = self.getWeights(gameState, action)
    return features * weights

  def getFeatures(self, gameState, action):
    """
    Returns a counter of features for the state
    """
    features = util.Counter()
    successor = self.getSuccessor(gameState, action)
    features['successorScore'] = self.getScore(successor)
    return features

  def getWeights(self, gameState, action):
    """
    Normally, weights do not depend on the gamestate.  They can be either
    a counter or a dictionary.
    """
    return {'successorScore': 1.0}

  def isOurTerrain(self, gameState, pos):
    """
    Check if pos is in our terrain
    """
    if self.red:
      return gameState.isRed(pos)
    else:
      return not gameState.isRed(pos)

  def setNeighbors(self, gameState):
    """
    this will set self.availableActions
    this is different from legalActions, which is for agents. this is for each grid and, thus, static
    """
    self.neighbors = util.Counter()
    for pos in self.legalPositions:
      self.neighbors[pos] = Actions.getLegalNeighbors(pos, gameState.data.layout.walls)

  def setBoundary(self, gameState):
    """
    set boundary in self.boudary, which is positions in our terrain which near the enemy side
    """
    self.boundary = []
    for pos in self.offensiveLegalPositions:
      for neighbor in self.neighbors[pos]:
        if self.isOurTerrain(gameState, neighbor):
	  self.boundary.append(neighbor)

  def mostLikelyPosition(self, gameState, positions, i = None):
    """
    find the max belief position in positions, i set the agent number
    if i == None, it finds global most likely position among all the agents
    return (position, prob)
    """
    maxPosition = None
    belief = -1 # make sure it can be replaced by someone
    if i != None:
      for pos in positions:
        if beliefs[i][pos] > belief:
	  belief = beliefs[i][pos]
	  maxPosition = pos
      return (maxPosition, belief)
    else:
      for i in self.getOpponents(gameState):
        currentPos = self.mostLikelyPosition(gameState, positions, i)
	if currentPos[1] > belief:
	  maxPosition = currentPos[0]
	  belief = currentPos[1]
      return (maxPosition, belief)

class OffensiveReflexAgent(ReflexCaptureAgent):
  """
  A reflex agent that seeks food. This is an agent
  we give you to get an idea of what an offensive agent might look like,
  but it is by no means the best or only way to build an offensive agent.
  """

  def registerInitialState(self, gameState):
    """
    Initialize the beliefs of the enemies uniformly
    and many related operations
    """
    ReflexCaptureAgent.registerInitialState(self, gameState)

    self.defending = False # initialy, it's not defending
    self.alert = 5

  def getFeatures(self, gameState, action):
    if invaded and self.isOurTerrain(gameState, gameState.getAgentState(self.index).getPosition()):
      self.defending = True

    if not invaded:
      self.defending = False
    
    if self.defending:
      # defending!
      return DefensiveReflexAgent.staticGetFeatures(self, gameState, action)

    # offending!
    features = util.Counter()
    successor = self.getSuccessor(gameState, action)
    enemies = [successor.getAgentState(i) for i in self.getOpponents(successor)]
    ghosts = [a for a in enemies if not a.isPacman and a.getPosition() != None and a.scaredTimer == 0]
    features['successorScore'] = self.getScore(successor)
    
    # Compute distance to the nearest food
    foodList = self.getFood(successor).asList()
    myPos = successor.getAgentState(self.index).getPosition()
    if len(foodList) > 0: # This should always be True,  but better safe than sorry
      minDistance = min([self.getMazeDistance(myPos, food) for food in foodList])
      features['distanceToFood'] = minDistance

    # award picking capsule
    features['capsules'] = len(self.getCapsules(successor))

    # using minimax to find how danger the current position is
    if not self.isOurTerrain(successor, myPos):
      if invaded:
        # it's okay for him to be killed and go back to reinforce defendenc
        self.maxDepth = 1
      else:
        self.maxDepth = 3

      ghostsPositions = [ghost.getPosition() for ghost in ghosts]
      features['danger'] = self.calculateDanger(successor, 0)
      # print features['danger'], action

    return features

  def calculateDanger(self, gameState, depth):
    myPos = gameState.getAgentState(self.index).getPosition()
    ghosts = util.Counter()

    for index in self.getOpponents(gameState):
      enemy = gameState.getAgentState(index)
      if not enemy.isPacman and enemy.getPosition() != None and enemy.scaredTimer == 0:
        ghosts[index] = enemy
      
    if self.isOurTerrain(gameState, myPos) and not (myPos in self.boundary):
      # back to our terrain? got killed
      # if on boundary, that's just retreat
      # printIndent(depth)
      # print "killed"
      return self.alert # very danger
    else:
      if ghosts.values() == []:
        # well, no observable ghosts, we're safe now
        return 0
      else:
        # there are observable ghosts to consider
        if depth < self.maxDepth:
	  # okay, now move ghosts
	  # there're observable ghosts
          for index, enemy in ghosts.items():
	    ghostAction = None
	    minDistance = 99999

	    actions = gameState.getLegalActions(index)
	    # move ghosts one step further to the pacman ('STOP' is a legal action)
	    # find the best action for ghost
	    for action in actions:
	      successor = gameState.generateSuccessor(index, action)
	      distance = self.getMazeDistance(successor.getAgentPosition(index), myPos)
	      if distance < minDistance:
		minDistance = distance
		ghostAction = action

	    # move it
	    gameState = gameState.generateSuccessor(index, ghostAction)

	  # we need to check which action is best, and return its danger (least is best)
	  # respond to ghost's move
	  actions = gameState.getLegalActions(self.index)
	  leastDanger = self.alert
	  for action in actions:
	    successor = self.getSuccessor(gameState, action)
	    newPos = gameState.getAgentState(self.index).getPosition()
	    danger = self.calculateDanger(successor, depth + 1)

	    if danger < leastDanger:
	      leastDanger = danger

	  # printIndent(depth)
	  # print leastDanger
	  return leastDanger # report distance no more than alert
	else:
	  # last layer
	  nearGhost = min([ghost.getPosition() for ghost in ghosts.values()], key=lambda x: self.getMazeDistance(x, myPos))
	  distance = self.getMazeDistance(nearGhost, myPos)
	  danger = max(self.alert - distance, 0)
	  # printIndent(depth)
	  # print danger
	  return danger
  
  def getWeights(self, gameState, action):
    if self.defending:
      # defend!
      return DefensiveReflexAgent.staticGetWeights(self, gameState, action)

    # offend!
    return {'successorScore': 100, 'distanceToFood': -1, 'capsules': -100, 'danger': -50}

class DefensiveReflexAgent(ReflexCaptureAgent):
  """
  A reflex agent that keeps its side Pacman-free. Again,
  this is to give you an idea of what a defensive agent
  could be like.  It is not the best or only way to make
  such an agent.
  """

  def getFeatures(self, gameState, action):
    global invaded

    features = util.Counter()
    successor = self.getSuccessor(gameState, action)

    myState = successor.getAgentState(self.index)
    myPos = myState.getPosition()

    # Computes whether we're on defense (1) or offense (0), encourage it to defense
    features['onDefense'] = 1
    if myState.isPacman: features['onDefense'] = 0

    # here are tupes with (position, prob)
    pacmanPos = self.mostLikelyPosition(successor, self.defensiveLegalPositions)
    ghostPos = self.mostLikelyPosition(successor, self.offensiveLegalPositions)
    if pacmanPos[1] != 0:
      # sensitive to pacman - any clue is regarded as invaded
      aimPosition = pacmanPos[0]
      # here, if the agent is invaded, send this signal to global context
      # offender, knowing this, might eat dangerous(deep in maze) dots, thus got killed, and came back for help!
      invaded = True
    else:
      aimPosition = ghostPos[0]
      invaded = False

    if self.isOurTerrain(successor, aimPosition) and self.isOurTerrain(successor, myPos):
      likelyDistance = self.defensiveDistancer.getDistance(myPos, aimPosition)
    else:
      likelyDistance = self.getMazeDistance(myPos, aimPosition)

    features['invaderDistance'] = likelyDistance

    if action == Directions.STOP: features['stop'] = 1
    rev = Directions.REVERSE[gameState.getAgentState(self.index).configuration.direction]
    if action == rev: features['reverse'] = 1

    return features

  staticGetFeatures = staticmethod(getFeatures)

  def getWeights(self, gameState, action):
    return {'onDefense': 10000, 'invaderDistance': -1, 'stop': -100, 'reverse': -2}
  staticGetWeights = staticmethod(getWeights)

def printIndent(num):
  for i in range(num + 1): print "   ",
