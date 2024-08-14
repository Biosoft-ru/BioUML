package biouml.plugins.enrichment;

import gnu.trove.list.TDoubleList;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class GroupInfo
{
    private final BitSet ranks;
    private int size = 0;
    private final int maxRank;
    private double ES;
    private double NES;
    private double NP;
    private int[] ranksArray;
    private int rankAtMax;
    
    public GroupInfo(int maxRank)
    {
        ranks = new BitSet(maxRank);
        this.maxRank = maxRank;
    }
    
    public void put(int rank)
    {
        if(!ranks.get(rank)) size++;
        ranks.set(rank);
    }
    
    /**
     * @return sorted array of gene ranks included into this group
     * Note that after first call of this function put(rank) will not work anymore
     */
    public int[] getRank()
    {
        if(ranksArray == null)
        {
            ranksArray = new int[size];
            int from = 0;
            for(int i=0; i<size; i++)
            {
                from = ranks.nextSetBit(from);
                ranksArray[i] = from++;
            }
        }
        return ranksArray;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public double getES()
    {
        return ES;
    }
    
    public double getNES()
    {
        return NES;
    }
    
    public double getNP()
    {
        return NP;
    }
    
    /**
     * Calculates average ES value for given maxRank and size with 3% accuracy for maxRank < 50000
     * @param maxRank
     * @param size
     * @return
     */
    public static double getAverageES(int maxRank, int size)
    {
        // Pre-calculated values based on 500000 permutations
        // values[maxRank-7][size-3] = KS(maxRank, size)
        double[][] values = new double[][]{
        {0.5598}, {0.5401}, {0.5200, 0.4998}, {0.5168, 0.4839}, {0.5090, 0.4740, 0.4569}, {0.4999, 0.4599, 0.4454},
                {0.4982, 0.4575, 0.4356, 0.4239}, {0.4942, 0.4518, 0.4278, 0.4134}, {0.4893, 0.4471, 0.4173, 0.4050, 0.3975},
                {0.4881, 0.4414, 0.4155, 0.3992, 0.3896}, {0.4863, 0.4403, 0.4116, 0.3927, 0.3821, 0.3750},
                {0.4822, 0.4371, 0.4079, 0.3854, 0.3760, 0.3686}, {0.4811, 0.4343, 0.4042, 0.3836, 0.3709, 0.3624, 0.3571},
                {0.4801, 0.4310, 0.4001, 0.3803, 0.3660, 0.3561, 0.3509}, {0.4778, 0.4302, 0.3986, 0.3770, 0.3599, 0.3523, 0.3453, 0.3411},
                {0.4772, 0.4284, 0.3968, 0.3744, 0.3583, 0.3475, 0.3407, 0.3358},
                {0.4756, 0.4266, 0.3941, 0.3714, 0.3559, 0.3439, 0.3362, 0.3311, 0.3268},
                {0.4740, 0.4246, 0.3928, 0.3682, 0.3532, 0.3385, 0.3321, 0.3263, 0.3225}, {0.4734, 0.4243, 0.3900, 0.3677},
                {0.4727, 0.4225, 0.3898}, {0.4714, 0.4221}, {0.4712, 0.4204}, {0.4708, 0.4199}, {0.4694, 0.4193}, {0.4691, 0.4184},
                {0.4686, 0.4172}, {0.4674, 0.4168}, {0.4674}, {0.4673}, {0.4663}, {0.4666}, {0.4663}, {0.4654}, {0.4652}, {0.4646},
                {0.4647}, {0.4638}, {0.4639}, {0.4634}, {0.4636}, {0.4633}, {0.4626}, {0.4626}, {0.4625}, {0.4617}, {0.4617}, {0.4616},
                {0.4616}, {0.4609}, {0.4610}, {0.4610}, {0.4606}, {0.4604}, {0.4603}, {0.4599}, {0.4600}, {0.4598}, {0.4600}, {0.4597},
                {0.4600}, {0.4596}, {0.4593}, {0.4594}, {0.4591}, {0.4587}, {0.4585}};
        // mids[size-3] = KS(size*2, size)
        // Predefined values based on 500000 permutations
        double[] mids = new double[] {0.5665, 0.5073, 0.4634, 0.4288, 0.4015, 0.3786, 0.3599, 0.3436, 0.3293, 0.3165, 0.3057, 0.2957,
                0.2863, 0.2782, 0.2710, 0.2637, 0.2574, 0.2514};
        if(size <= 0 || size >= maxRank) return 0;
        if(size > maxRank/2) size = maxRank - size;
        if(size == 1)   // Exact formula for size = 1
        {
            if( maxRank % 2 != 0 )
                maxRank++;
            return (3.0*maxRank-2)/(4*maxRank-4);
        }
        if(size == 2)   // Exact formula for size = 2
        {
            if( maxRank % 2 != 0 )
                return ( 13.0 * maxRank * maxRank - 17.0 * maxRank - 6 ) / ( 24.0 * maxRank * ( maxRank - 2 ) );
            return (13.0*maxRank-4)/(24*(maxRank-1));
        }
        // Predefined values for maxRank = size*2
        if(size*2 == maxRank && size-3 < mids.length) return mids[size-3];
        if(maxRank-7 < values.length && size-3 < values[maxRank-7].length) return values[maxRank-7][size-3];
        // Empirically defined formula. Returns good result with max 3% inaccuracy for maxRank>28 & size>2.
        if((double)size*(maxRank-size)/maxRank < 266)
            return 0.779/Math.pow((double)size*(maxRank-size)/maxRank, 0.483);
        return 0.833/Math.pow((double)size*(maxRank-size)/maxRank, 0.495);
    }

    public void calculateScore()
    {
        long curES = 0;
        long maxES = 0;
        long addValue = maxRank-getSize();
        if(addValue == 0)
        {
            ES = 0;
            rankAtMax = 0;
            return;
        }
        long subtractValue = getSize();
        long ESdenominator = addValue * subtractValue;
        int bestPos = 0;
        if(subtractValue < maxRank/4)
        {
            int lastRank = -1;
            for(int curRank: getRank())
            {
                curES -= subtractValue * (curRank-lastRank-1);
                if(Math.abs(curES) > Math.abs(maxES))
                {
                    bestPos = curRank;
                    maxES = curES;
                }
                curES += addValue;
                if(Math.abs(curES) > Math.abs(maxES))
                {
                    bestPos = curRank;
                    maxES = curES;
                }
                lastRank = curRank;
            }
            curES -= subtractValue * (maxRank-lastRank-1);
            if(Math.abs(curES) > Math.abs(maxES))
            {
                bestPos = maxRank;
                maxES = curES;
            }
        } else
        {
            for(int i=0; i<maxRank; i++)
            {
                if( this.ranks.get(i) )
                    curES += addValue;
                else
                    curES -= subtractValue;
                if(Math.abs(curES) > Math.abs(maxES))
                {
                    bestPos = i;
                    maxES = curES;
                }
            }
        }
        ES = (double)maxES/ESdenominator;
        rankAtMax = bestPos;
    }
    
    public double[][] getScorePlot()
    {
        List<double[]> result = new ArrayList<>();
        long curES = 0;
        long addValue = maxRank-getSize();
        long subtractValue = getSize();
        long ESdenominator = addValue * subtractValue;
        result.add(new double[] {0,0});
        int lastRank = -1;
        for(int curRank: getRank())
        {
            if(curRank-lastRank-1 > 0)
            {
                curES -= subtractValue * (curRank-lastRank-1);
                result.add(new double[]{curRank, (double)curES/ESdenominator});
            }
            curES += addValue;
            result.add(new double[]{curRank+1, (double)curES/ESdenominator});
            lastRank = curRank;
        }
        if(maxRank-lastRank-1 > 0)
        {
            curES -= subtractValue * (maxRank-lastRank-1);
            result.add(new double[]{maxRank, (double)curES/ESdenominator});
        }
        return result.toArray(new double[result.size()][]);
    }
    
    public double[] getESValues()
    {
        double[] result = new double[maxRank];
        long curES = 0;
        long addValue = maxRank-this.ranks.size();
        long subtractValue = this.ranks.size();
        long ESdenominator = addValue * subtractValue;
        int lastRank = -1;
        for(int curRank: getRank())
        {
            if(curRank-lastRank-1 > 0)
            {
                for(int i = lastRank+1; i < curRank; i++ )
                {
                    curES -= subtractValue;
                    result[i] = (double)curES/ESdenominator;
                }
            }
            curES += addValue;
            result[curRank] = (double)curES/ESdenominator;
            lastRank = curRank;
        }
        if(maxRank-lastRank-1 > 0)
        {
            for(int i = lastRank+1; i < maxRank; i++ )
            {
                curES -= subtractValue;
                result[i] = (double)curES/ESdenominator;
            }
        }
        return result;
    }
    
    public void calculatePValue(int nGroups, TDoubleList nesList, TDoubleList nesRandomList)
    {
        double sumES = 0;
        int nES = 0;
        int NPcount = 0;
        if(getSize() == maxRank)
        {
            NP = 1.0;
            NES = Double.NaN;
            return;
        }
        Random random = new Random();
        double[] esValues = new double[nGroups];
        for(int i=0; i<nGroups; i++)
        {
            GroupInfo group = new GroupInfo(maxRank);
            do
            {
                group.put(random.nextInt(maxRank));
            }
            while( group.getSize() < getSize() );
            group.calculateScore();
            esValues[i] = group.getES();
            // equal sign test
            if(ES>=0 ^ group.getES()<0)
            {
                sumES += group.getES();
                nES++;
                if(Math.abs(group.getES()) > Math.abs(ES))
                {
                    NPcount++;
                }
            }
        }
        NES = ES*nES/sumES;
        NP = (double)NPcount / nGroups;
        nesList.add(NES);
        for(double esValue: esValues)
            nesRandomList.add(esValue*nES/sumES);
    }

    public int getRankAtMax()
    {
        return rankAtMax;
    }
}