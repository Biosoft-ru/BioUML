package biouml.plugins.riboseq.mappability;

/*
 * sais.java for sais-java
 * Copyright (c) 2008-2010 Yuta Mori All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

public class SuffixArraysLong {
    private static interface BaseArray
    {
        public long get(long i);
        public void set(long i, long val);
        public long update(long i, long val);
    }
    private static final int JVM_MAX_ARRAY_SIZE = Integer.MAX_VALUE - 10;//JVM can not allocate array of Integer.MAX_VALUE size, but a bit smaller is ok.
    
    public static class ByteArray implements BaseArray
    {
        private byte[][] m_A = null;
        public ByteArray(long capacity)
        {
            if(capacity % JVM_MAX_ARRAY_SIZE == 0)
                m_A = new byte[(int)(capacity / JVM_MAX_ARRAY_SIZE)][JVM_MAX_ARRAY_SIZE];
            else
            {
                m_A = new byte[(int)(capacity / JVM_MAX_ARRAY_SIZE +  1)][];
                for(int i = 0; i < m_A.length - 1; i++)
                {
                    m_A[i] = new byte[JVM_MAX_ARRAY_SIZE];
                }
                m_A[m_A.length - 1] = new byte[(int)(capacity % JVM_MAX_ARRAY_SIZE)];
            }
        }
        @Override
        public long get(long i)
        {
            return m_A[row(i)][col(i)] & 0xff;
        }
        @Override
        public void set(long i, long val)
        {
            m_A[row(i)][col(i)] = (byte) ( val & 0xff );
        }
        @Override
        public long update(long i, long val)
        {
            return m_A[row(i)][col(i)] += (byte) (val & 0xff);
        }
        
        private int row(long pos) { return (int)(pos / JVM_MAX_ARRAY_SIZE); }
        private int col(long pos) { return (int)(pos % JVM_MAX_ARRAY_SIZE); }
    }
    public static class LongArray implements BaseArray
    {
        long[][] m_A = null;
        long m_pos = 0;
        public LongArray(long capacity)
        {
            if(capacity % JVM_MAX_ARRAY_SIZE == 0)
                m_A = new long[(int)(capacity / JVM_MAX_ARRAY_SIZE)][JVM_MAX_ARRAY_SIZE];
            else
            {
                m_A = new long[(int)(capacity / JVM_MAX_ARRAY_SIZE +  1)][];
                for(int i = 0; i < m_A.length - 1; i++)
                {
                    m_A[i] = new long[JVM_MAX_ARRAY_SIZE];
                }
                m_A[m_A.length - 1] = new long[(int)(capacity % JVM_MAX_ARRAY_SIZE)];
            }
        }
        LongArray(long[][] A, long pos)
        {
            m_A = A;
            m_pos = pos;
        }
        @Override
        public long get(long i)
        {
            return m_A[row(i)][col(i)];
        }
        @Override
        public void set(long i, long val)
        {
            m_A[row(i)][col(i)] = val;
        }
        @Override
        public long update(long i, long val)
        {
            return m_A[row(i)][col(i)] += val;
        }
        private int row(long pos) { return (int)((pos+m_pos) / JVM_MAX_ARRAY_SIZE); }
        private int col(long pos) { return (int)((pos+m_pos) % JVM_MAX_ARRAY_SIZE); }

    }

  /* find the start or end of each bucket */
  private static
  void
  getCounts(BaseArray T, BaseArray C, long n, long k) {
    long i;
    for(i = 0; i < k; ++i) { C.set(i, 0); }
    for(i = 0; i < n; ++i) { C.update(T.get(i), 1); }
  }
  private static
  void
  getBuckets(BaseArray C, BaseArray B, long k, boolean end) {
    long i, sum = 0;
    if(end != false) { for(i = 0; i < k; ++i) { sum += C.get(i); B.set(i, sum); } }
    else { for(i = 0; i < k; ++i) { sum += C.get(i); B.set(i, sum - C.get(i)); } }
  }

  /* sort all type LMS suffixes */
  private static
  void
  LMSsort(BaseArray T, BaseArray SA, BaseArray C, BaseArray B, long n, long k) {
    long b, i, j;
    long c0, c1;
    /* compute SAl */
    if(C == B) { getCounts(T, C, n, k); }
    getBuckets(C, B, k, false); /* find starts of buckets */
    j = n - 1;
    b = B.get(c1 = T.get(j));
    --j;
    SA.set( b++, (T.get(j) < c1) ? ~j : j );
    for(i = 0; i < n; ++i) {
      if(0 < (j = SA.get( i ))) {
          if((c0 = T.get(j)) != c1) { B.set(c1, b); b = B.get(c1 = c0); }
        --j;
        SA.set( b++, (T.get(j) < c1) ? ~j : j );
        SA.set( i, 0 );
      } else if(j < 0) {
        SA.set( i, ~j );
      }
    }
    /* compute SAs */
    if(C == B) { getCounts(T, C, n, k); }
    getBuckets(C, B, k, true); /* find ends of buckets */
    for(i = n - 1, b = B.get(c1 = 0); 0 <= i; --i) {
      if(0 < (j = SA.get( i ))) {
        if((c0 = T.get(j)) != c1) { B.set(c1, b); b = B.get(c1 = c0); }
        --j;
        SA.set( --b, (T.get(j) > c1) ? ~(j + 1) : j );
        SA.set( i, 0 );
      }
    }
  }
  private static
  long
  LMSpostproc(BaseArray T, BaseArray SA, long n, long m) {
    long i, j, p, q, plen, qlen, name;
    long c0, c1;
    boolean diff;

    /* compact all the sorted substrings into the first m items of SA
        2*m must be not larger than n (proveable) */
    for(i = 0; (p = SA.get( i )) < 0; ++i) { SA.set( i, ~p ); }
    if(i < m) {
      for(j = i, ++i;; ++i) {
        if((p = SA.get(i)) < 0) {
          SA.set( j++, ~p );
          SA.set( i, 0 );
          if(j == m) { break; }
        }
      }
    }

    /* store the length of all substrings */
    i = n - 1; j = n - 1; c0 = T.get(n - 1);
    do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
    for(; 0 <= i;) {
      do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) <= c1));
      if(0 <= i) {
        SA.set( m + ((i + 1) >> 1), j - i );
        j = i + 1;
        do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
      }
    }

    /* find the lexicographic names of all substrings */
    for(i = 0, name = 0, q = n, qlen = 0; i < m; ++i) {
      p = SA.get( i ); plen = SA.get( m + (p >> 1)); diff = true;
      if((plen == qlen) && ((q + plen) < n)) {
        for(j = 0; (j < plen) && (T.get(p + j) == T.get(q + j)); ++j) { }
        if(j == plen) { diff = false; }
      }
      if(diff != false) { ++name; q = p; qlen = plen; }
      SA.set( m + (p >> 1), name );
    }

    return name;
  }

  /* compute SA and BWT */
  private static
  void
  induceSA(BaseArray T, BaseArray SA, BaseArray C, BaseArray B, long n, long k) {
    long b, i, j;
    long c0, c1;
    /* compute SAl */
    if(C == B) { getCounts(T, C, n, k); }
    getBuckets(C, B, k, false); /* find starts of buckets */
    j = n - 1;
    b = B.get(c1 = T.get(j));
    SA.set( b++, ((0 < j) && (T.get(j - 1) < c1)) ? ~j : j );
    for(i = 0; i < n; ++i) {
      j = SA.get( i ); SA.set( i, ~j );
      if(0 < j) {
        if((c0 = T.get(--j)) != c1) { B.set(c1, b); b = B.get(c1 = c0); }
        SA.set( b++, ((0 < j) && (T.get(j - 1) < c1)) ? ~j : j );
      }
    }
    /* compute SAs */
    if(C == B) { getCounts(T, C, n, k); }
    getBuckets(C, B, k, true); /* find ends of buckets */
    for(i = n - 1, b = B.get(c1 = 0); 0 <= i; --i) {
      if(0 < (j = SA.get( i ))) {
        if((c0 = T.get(--j)) != c1) { B.set(c1, b); b = B.get(c1 = c0); }
        SA.set( --b, ((j == 0) || (T.get(j - 1) > c1)) ? ~j : j );
      } else {
        SA.set( i, ~j );
      }
    }
  }

  /* find the suffix array SA of T[0..n-1] in {0..k-1}^n
     use a working space (excluding T and SA) of at most 2n+O(1) for a constant alphabet */
  private static
  void
  SA_IS(BaseArray T, LongArray SA, long fs, long n, long k) {
    BaseArray C, B, RA;
    long i, j, b, c, m, p, q, name, newfs;
    long c0, c1;
    long flags = 0;

    if(k <= 256) {
      C = new LongArray(k);
      if(k <= fs) { B = new LongArray(SA.m_A, n + fs - k); flags = 1; }
      else { B = new LongArray(k); flags = 3; }
    } else if(k <= fs) {
      C = new LongArray(SA.m_A, n + fs - k);
      if(k <= (fs - k)) { B = new LongArray(SA.m_A, n + fs - k * 2); flags = 0; }
      else if(k <= 1024) { B = new LongArray(k); flags = 2; }
      else { B = C; flags = 8; }
    } else {
      C = B = new LongArray(k);
      flags = 4 | 8;
    }

    /* stage 1: reduce the problem by at least 1/2
       sort all the LMS-substrings */
    getCounts(T, C, n, k); getBuckets(C, B, k, true); /* find ends of buckets */
    for(i = 0; i < n; ++i) { SA.set( i, 0 ); }
    b = -1; i = n - 1; j = n; m = 0; c0 = T.get(n - 1);
    do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
    for(; 0 <= i;) {
      do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) <= c1));
      if(0 <= i) {
        if(0 <= b) { SA.set( b, j ); } b = B.update(c1, -1); j = i; ++m;
        do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
      }
    }
    if(1 < m) {
      LMSsort(T, SA, C, B, n, k);
      name = LMSpostproc(T, SA, n, m);
    } else if(m == 1) {
      SA.set( b, j + 1 );
      name = 1;
    } else {
      name = 0;
    }

    /* stage 2: solve the reduced problem
       recurse if names are not yet unique */
    if(name < m) {
      if((flags & 4) != 0) { C = null; B = null; }
      if((flags & 2) != 0) { B = null; }
      newfs = (n + fs) - (m * 2);
      if((flags & (1 | 4 | 8)) == 0) {
        if((k + name) <= newfs) { newfs -= k; }
        else { flags |= 8; }
      }
      for(i = m + (n >> 1) - 1, j = m * 2 + newfs - 1; m <= i; --i) {
        if(SA.get( i ) != 0) { SA.set( j--, SA.get( i ) - 1 ); }
      }
      RA = new LongArray(SA.m_A, m + newfs);
      SA_IS(RA, SA, newfs, m, name);
      RA = null;

      i = n - 1; j = m * 2 - 1; c0 = T.get(n - 1);
      do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
      for(; 0 <= i;) {
        do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) <= c1));
        if(0 <= i) {
          SA.set( j--, i+1 );
          do { c1 = c0; } while((0 <= --i) && ((c0 = T.get(i)) >= c1));
        }
      }

      for(i = 0; i < m; ++i) { SA.set( i, SA.get( m + SA.get( i ) ) ); }
      if((flags & 4) != 0) { C = B = new LongArray(k); }
      if((flags & 2) != 0) { B = new LongArray(k); }
    }

    /* stage 3: induce the result for the original problem */
    if((flags & 8) != 0) { getCounts(T, C, n, k); }
    /* put all left-most S characters into their buckets */
    if(1 < m) {
      getBuckets(C, B, k, true); /* find ends of buckets */
      i = m - 1; j = n; p = SA.get( m - 1 ); c1 = T.get(p);
      do {
        q = B.get(c0 = c1);
        while(q < j) { SA.set( --j, 0 ); }
        do {
          SA.set( --j, p );
          if(--i < 0) { break; }
          p = SA.get( i );
        } while((c1 = T.get(p)) == c0);
      } while(0 <= i);
      while(0 < j) { SA.set( --j, 0 ); }
    }
    induceSA(T, SA, C, B, n, k);
    C = null; B = null;
  }

    public static void suffixsort(ByteArray t, LongArray sa, long n)
    {
        if( t == null || sa == null  )
            throw new IllegalArgumentException();
        if( n <= 1 )
        {
            if( n == 1 )
                sa.set( 0, 0 );
            return;
        }
        SA_IS( t, sa, 0, n, 256 );
    }
    
    public static boolean check(ByteArray t, LongArray sa, long n)
    {
        LongArray isa = new LongArray(n);
        for(long i = 0; i < n; i++)
            isa.set( i, -1 );
        for(long i = 0; i < n; i++)
        {
            long x = sa.get( i );
            if(x < 0 || x >= n)
                return false;
            isa.set( x, i );
        }
        for(long i = 0; i < n; i++)
            if(isa.get( i ) == -1)
                return false;

        for(long i = 1; i < n; i++)
        {
            long a = sa.get( i - 1 );
            long b = sa.get( i );
            if(t.get( a ) > t.get( b ))
                return false;
            if(t.get( a ) == t.get( b ) && a != n-1)
            {
                if( isa.get( a+1 ) > isa.get( b+1 ) )
                    return false;
            }
        }
        return true;
    }
    
    /**
     * Compute longest common prefix array from suffix array.
     * The '\n' character is specially treated as a word separator.
     * The lcp array is one item longer then sa and the last value is 0 for convinience.  
     */
    public static LongArray computeLCP(ByteArray t, LongArray sa, long n)
    {
        LongArray isa = new LongArray(n);
        for(long i = 0; i < n; i++)
            isa.set( sa.get( i ), i );
        LongArray lcp = new LongArray( n + 1 );
        long d = 0;
        for( long i = 0; i < n; i++ )
        {
            if(d > 0) --d;
            long j = isa.get( i );
            if(j == 0)
            {
                d = 0;
                lcp.set( j, 0 );
                continue;
            }
            long prev = sa.get(j-1);
            long next = sa.get( j );
            while( t.get( prev + d ) == t.get( next + d ) && t.get( prev + d ) != '\n' )
                d++;
            lcp.set( j, d );
        }
        return lcp;
    }
}